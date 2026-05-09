package io.resrv.application.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.auth.RoleNames;
import io.resrv.application.auth.out.TokenGenerationPort;
import io.resrv.application.auth.out.TokenResult;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.out.CustomerCommandPort;
import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.TenantNotFoundException;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.TenantStatus;
import io.resrv.domain.tenant.Timezone;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CustomerServiceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final UUID CUSTOMER_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");

    private TenantQueryPort tenantQueryPort;
    private CustomerCommandPort customerCommandPort;
    private CustomerQueryPort customerQueryPort;
    private PasswordHashingPort passwordHashingPort;
    private TokenGenerationPort tokenGenerationPort;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        tenantQueryPort = mock(TenantQueryPort.class);
        customerCommandPort = mock(CustomerCommandPort.class);
        customerQueryPort = mock(CustomerQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        tokenGenerationPort = mock(TokenGenerationPort.class);
        when(passwordHashingPort.hash("constant-time-customer-dummy")).thenReturn("dummy-hash");
        service =
                new CustomerService(
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        tenantQueryPort,
                        customerCommandPort,
                        customerQueryPort,
                        passwordHashingPort,
                        tokenGenerationPort);
    }

    @Test
    void register_hashesPasswordAndSavesTenantScopedCustomer() {
        when(tenantQueryPort.findBySlug("my-salon")).thenReturn(Optional.of(activeTenant()));
        when(customerQueryPort.existsByTenantIdAndEmail(
                        TENANT_ID, new CustomerEmail("customer@example.com")))
                .thenReturn(false);
        when(passwordHashingPort.hash("plain-password")).thenReturn("hashed-password");

        final var result =
                service.register(
                        new RegisterCustomerCommand(
                                "my-salon",
                                " Customer@Example.com ",
                                " Jane Customer ",
                                "plain-password"));

        assertEquals(TENANT_ID.value(), result.tenantId());
        assertEquals("customer@example.com", result.email());
        assertEquals("Jane Customer", result.name());
        assertTrue(result.active());
        assertEquals(FIXED_NOW, result.createdAt());

        final var captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerCommandPort).save(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().tenantId());
        assertEquals("hashed-password", captor.getValue().hashedPassword());
        verify(passwordHashingPort).hash("plain-password");
    }

    @Test
    void register_unknownTenant_throwsBeforeSaving() {
        when(tenantQueryPort.findBySlug("missing")).thenReturn(Optional.empty());

        assertThrows(
                TenantNotFoundException.class,
                () ->
                        service.register(
                                new RegisterCustomerCommand(
                                        "missing",
                                        "customer@example.com",
                                        "Jane Customer",
                                        "plain-password")));

        verify(customerCommandPort, never()).save(any());
        verify(customerQueryPort, never()).existsByTenantIdAndEmail(any(), any());
        verify(passwordHashingPort, never()).hash("plain-password");
    }

    @Test
    void register_duplicateEmail_throwsBeforeHashingRawPassword() {
        when(tenantQueryPort.findBySlug("my-salon")).thenReturn(Optional.of(activeTenant()));
        when(customerQueryPort.existsByTenantIdAndEmail(
                        TENANT_ID, new CustomerEmail("customer@example.com")))
                .thenReturn(true);

        assertThrows(
                CustomerEmailAlreadyExistsException.class,
                () ->
                        service.register(
                                new RegisterCustomerCommand(
                                        "my-salon",
                                        "customer@example.com",
                                        "Jane Customer",
                                        "plain-password")));

        verify(customerCommandPort, never()).save(any());
        verify(passwordHashingPort, never()).hash("plain-password");
    }

    @Test
    void register_blankPassword_failsBeforePortLookup() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.register(
                                new RegisterCustomerCommand(
                                        "my-salon", "customer@example.com", "Jane", " ")));

        verifyNoInteractions(
                tenantQueryPort, customerCommandPort, customerQueryPort, tokenGenerationPort);
    }

    @Test
    void login_success_generatesCustomerRoleToken() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(TENANT_ID));
        when(customerQueryPort.findCredentialsByTenantIdAndEmail(TENANT_ID, "customer@example.com"))
                .thenReturn(
                        Optional.of(
                                new UserCredentials(
                                        CUSTOMER_UUID,
                                        TENANT_ID.value(),
                                        "hashed-password",
                                        RoleNames.CUSTOMER,
                                        true)));
        when(passwordHashingPort.matches("plain-password", "hashed-password")).thenReturn(true);
        when(tokenGenerationPort.generate(CUSTOMER_UUID, TENANT_ID.value(), RoleNames.CUSTOMER))
                .thenReturn(new TokenResult("access-token", 3600));

        final var result =
                service.login(
                        new CustomerLoginCommand(
                                "my-salon", "Customer@Example.com", "plain-password"));

        assertEquals("access-token", result.accessToken());
        assertEquals(3600, result.expiresIn());
        verify(tokenGenerationPort).generate(CUSTOMER_UUID, TENANT_ID.value(), RoleNames.CUSTOMER);
    }

    @Test
    void login_unknownTenant_checksDummyHashAndFails() {
        when(tenantQueryPort.findIdBySlug("missing")).thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        service.login(
                                new CustomerLoginCommand(
                                        "missing", "customer@example.com", "plain-password")));

        verify(passwordHashingPort).matches("plain-password", "dummy-hash");
        verify(customerQueryPort, never()).findCredentialsByTenantIdAndEmail(any(), any());
        verifyNoInteractions(tokenGenerationPort);
    }

    @Test
    void login_inactiveCustomer_failsBeforeTokenGeneration() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(TENANT_ID));
        when(customerQueryPort.findCredentialsByTenantIdAndEmail(TENANT_ID, "customer@example.com"))
                .thenReturn(
                        Optional.of(
                                new UserCredentials(
                                        CUSTOMER_UUID,
                                        TENANT_ID.value(),
                                        "hashed-password",
                                        RoleNames.CUSTOMER,
                                        false)));
        when(passwordHashingPort.matches("plain-password", "hashed-password")).thenReturn(true);

        assertThrows(
                AuthenticationFailedException.class,
                () ->
                        service.login(
                                new CustomerLoginCommand(
                                        "my-salon", "customer@example.com", "plain-password")));

        verifyNoInteractions(tokenGenerationPort);
    }

    @Test
    void login_blankCredentials_failBeforeLookup() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.login(new CustomerLoginCommand("my-salon", " ", "plain-password")));

        verifyNoInteractions(tenantQueryPort, customerQueryPort, tokenGenerationPort);
    }

    private static Tenant activeTenant() {
        return Tenant.reconstitute(
                TENANT_ID,
                new TenantName("My Salon"),
                new Slug("my-salon"),
                new Timezone(ZoneId.of("UTC")),
                new SlotDuration(60),
                new HoldTtl(15),
                new CancellationWindow(60),
                TenantStatus.ACTIVE,
                FIXED_NOW);
    }
}

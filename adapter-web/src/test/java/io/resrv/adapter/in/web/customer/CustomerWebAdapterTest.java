package io.resrv.adapter.in.web.customer;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.auth.LoginResult;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.CustomerLoginUseCase;
import io.resrv.application.customer.in.CustomerResult;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.in.RegisterCustomerUseCase;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerWebAdapter.class)
class CustomerWebAdapterTest {

    private static final String TENANT_SLUG = "test-salon";
    private static final UUID TENANT_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterCustomerUseCase registerCustomerUseCase;

    @MockitoBean private CustomerLoginUseCase customerLoginUseCase;

    @Test
    void register_success_returns201WithLocationAndTenantScopedCommand() throws Exception {
        when(registerCustomerUseCase.register(any(RegisterCustomerCommand.class)))
                .thenReturn(customerResult("customer@test.com", "Jane Customer"));

        mockMvc.perform(
                        post("/public/{tenantSlug}/customers", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "customer@test.com",
                                            "name": "Jane Customer",
                                            "password": "password123"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        "Location",
                                        "/public/%s/customers/%s"
                                                .formatted(TENANT_SLUG, CUSTOMER_ID_VALUE)))
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID_VALUE.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID_VALUE.toString()))
                .andExpect(jsonPath("$.email").value("customer@test.com"))
                .andExpect(jsonPath("$.name").value("Jane Customer"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value("2025-01-01T00:00:00Z"));

        final var captor = ArgumentCaptor.forClass(RegisterCustomerCommand.class);
        verify(registerCustomerUseCase).register(captor.capture());
        final var command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.tenantSlug()).isEqualTo(TENANT_SLUG);
        org.assertj.core.api.Assertions.assertThat(command.email()).isEqualTo("customer@test.com");
        org.assertj.core.api.Assertions.assertThat(command.name()).isEqualTo("Jane Customer");
        org.assertj.core.api.Assertions.assertThat(command.password()).isEqualTo("password123");
    }

    @Test
    void login_success_returns200WithCustomerToken() throws Exception {
        when(customerLoginUseCase.login(any(CustomerLoginCommand.class)))
                .thenReturn(new LoginResult("customer-token", 1800L));

        mockMvc.perform(
                        post("/public/{tenantSlug}/customers/login", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "customer@test.com",
                                            "password": "password123"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("customer-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(customerLoginUseCase)
                .login(new CustomerLoginCommand(TENANT_SLUG, "customer@test.com", "password123"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(registerCustomerUseCase.register(any(RegisterCustomerCommand.class)))
                .thenThrow(
                        new CustomerEmailAlreadyExistsException(
                                TenantId.of(TENANT_ID_VALUE),
                                new CustomerEmail("customer@test.com")));

        mockMvc.perform(
                        post("/public/{tenantSlug}/customers", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "customer@test.com",
                                            "name": "Jane Customer",
                                            "password": "password123"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Customer email 'customer@test.com' is already in use for tenant '%s'"
                                                .formatted(TENANT_ID_VALUE)));
    }

    @Test
    void login_authenticationFailed_returns401() throws Exception {
        when(customerLoginUseCase.login(any())).thenThrow(new AuthenticationFailedException());

        mockMvc.perform(
                        post("/public/{tenantSlug}/customers/login", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "customer@test.com",
                                            "password": "wrong"
                                        }
                                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
    }

    @Test
    void register_invalidRequest_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/public/{tenantSlug}/customers", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "not-an-email",
                                            "name": "",
                                            "password": "short"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(3)));
    }

    private static CustomerResult customerResult(final String email, final String name) {
        return new CustomerResult(CUSTOMER_ID_VALUE, TENANT_ID_VALUE, email, name, true, NOW);
    }
}

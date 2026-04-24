package io.resrv.application.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.application.admin.out.AdminCommandPort;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.in.RegisterTenantCommand;
import io.resrv.application.tenant.out.TenantCommandPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.admin.Admin;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import io.resrv.domain.tenant.Tenant;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterTenantServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");

    private TenantCommandPort tenantCommandPort;
    private AdminCommandPort adminCommandPort;
    private TenantQueryPort tenantQueryPort;
    private PasswordHashingPort passwordHashingPort;
    private RegisterTenantService service;

    @BeforeEach
    void setUp() {
        tenantCommandPort = mock(TenantCommandPort.class);
        adminCommandPort = mock(AdminCommandPort.class);
        tenantQueryPort = mock(TenantQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        final var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service =
                new RegisterTenantService(
                        clock,
                        tenantCommandPort,
                        adminCommandPort,
                        tenantQueryPort,
                        passwordHashingPort);
    }

    @Test
    void happyPath_allPortsCalled() {
        stubPortsForSuccess();

        final var command =
                new RegisterTenantCommand(
                        "마이살롱",
                        "my-salon",
                        ZoneId.of("Asia/Seoul"),
                        60,
                        15,
                        0,
                        "admin@example.com",
                        "password123");

        final var tenant = service.register(command);

        assertNotNull(tenant);
        assertEquals("마이살롱", tenant.name().value());
        assertEquals("my-salon", tenant.slug().value());

        verify(tenantCommandPort).save(any(Tenant.class));
        verify(adminCommandPort).save(any(Admin.class));
        verify(passwordHashingPort).hash("password123");
    }

    @Test
    void duplicateSlug_throwsException() {
        when(tenantQueryPort.existsBySlug(any())).thenReturn(true);

        final var command = validCommand();

        assertThrows(SlugAlreadyExistsException.class, () -> service.register(command));
        verify(tenantCommandPort, never()).save(any());
        verify(adminCommandPort, never()).save(any());
    }

    @Test
    void passwordHashingPortCalledWithRawPassword() {
        stubPortsForSuccess();

        final var command =
                new RegisterTenantCommand(
                        "Test",
                        "test-slug",
                        ZoneId.of("UTC"),
                        30,
                        5,
                        0,
                        "admin@example.com",
                        "mySecurePass");

        service.register(command);

        verify(passwordHashingPort).hash("mySecurePass");
    }

    @Test
    void omittedHoldTtl_defaultsTo15() {
        stubPortsForSuccess();

        final var command =
                new RegisterTenantCommand(
                        "Test",
                        "test-slug",
                        ZoneId.of("UTC"),
                        30,
                        null,
                        0,
                        "admin@example.com",
                        "password123");

        final var tenant = service.register(command);

        assertEquals(15, tenant.holdTtl().minutes());
    }

    @Test
    void omittedCancellationWindow_defaultsTo0() {
        stubPortsForSuccess();

        final var command =
                new RegisterTenantCommand(
                        "Test",
                        "test-slug",
                        ZoneId.of("UTC"),
                        30,
                        5,
                        null,
                        "admin@example.com",
                        "password123");

        final var tenant = service.register(command);

        assertEquals(0, tenant.cancellationWindow().minutes());
    }

    @Test
    void adminCreationFailure_tenantNotSaved() {
        stubPortsForSuccess();
        doThrow(new RuntimeException("DB error")).when(adminCommandPort).save(any());

        final var command = validCommand();

        assertThrows(RuntimeException.class, () -> service.register(command));
        // Note: actual rollback is verified in integration test with @Transactional
    }

    private void stubPortsForSuccess() {
        when(tenantQueryPort.existsBySlug(any())).thenReturn(false);
        when(passwordHashingPort.hash(anyString())).thenReturn("$argon2id$hashed");
    }

    private static RegisterTenantCommand validCommand() {
        return new RegisterTenantCommand(
                "마이살롱",
                "my-salon",
                ZoneId.of("Asia/Seoul"),
                60,
                15,
                0,
                "admin@example.com",
                "password123");
    }
}

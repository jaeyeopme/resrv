package io.resrv.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.application.admin.out.AdminQueryPort;
import io.resrv.application.auth.in.LoginCommand;
import io.resrv.application.auth.out.TokenGenerationPort;
import io.resrv.application.auth.out.TokenResult;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.tenant.TenantId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private TenantQueryPort tenantQueryPort;
    private AdminQueryPort adminQueryPort;
    private PasswordHashingPort passwordHashingPort;
    private TokenGenerationPort tokenGenerationPort;
    private LoginService service;

    private TenantId tenantId;
    private UUID userId;
    private UUID rawTenantId;
    private LoginCommand command;
    private UserCredentials activeCredentials;

    @BeforeEach
    void setUp() {
        tenantQueryPort = mock(TenantQueryPort.class);
        adminQueryPort = mock(AdminQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        tokenGenerationPort = mock(TokenGenerationPort.class);

        when(passwordHashingPort.hash("constant-time-dummy")).thenReturn("$argon2id$dummy-hash");

        service =
                new LoginService(
                        tenantQueryPort, adminQueryPort, passwordHashingPort, tokenGenerationPort);

        rawTenantId = UUID.randomUUID();
        tenantId = TenantId.of(rawTenantId);
        userId = UUID.randomUUID();
        command = new LoginCommand("my-salon", "admin@example.com", "password123");
        activeCredentials =
                new UserCredentials(userId, rawTenantId, "$argon2id$hashed", RoleNames.OWNER, true);
    }

    @Test
    void login_success() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(tenantId));
        when(adminQueryPort.findCredentialsByTenantIdAndEmail(tenantId, "admin@example.com"))
                .thenReturn(Optional.of(activeCredentials));
        when(passwordHashingPort.matches("password123", "$argon2id$hashed")).thenReturn(true);
        when(tokenGenerationPort.generate(userId, rawTenantId, RoleNames.OWNER))
                .thenReturn(new TokenResult("access-token-value", 3600L));

        final var result = service.login(command);

        assertEquals("access-token-value", result.accessToken());
        assertEquals(3600L, result.expiresIn());
        verify(tokenGenerationPort).generate(userId, rawTenantId, RoleNames.OWNER);
    }

    @Test
    void login_unknownTenant() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class, () -> service.login(command));

        verify(passwordHashingPort).matches("password123", "$argon2id$dummy-hash");
        verifyNoInteractions(adminQueryPort, tokenGenerationPort);
    }

    @Test
    void login_unknownEmail() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(tenantId));
        when(adminQueryPort.findCredentialsByTenantIdAndEmail(tenantId, "admin@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class, () -> service.login(command));

        verify(passwordHashingPort).matches("password123", "$argon2id$dummy-hash");
        verifyNoInteractions(tokenGenerationPort);
    }

    @Test
    void login_inactiveUser() {
        final var inactiveCredentials =
                new UserCredentials(
                        userId, rawTenantId, "$argon2id$hashed", RoleNames.OWNER, false);
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(tenantId));
        when(adminQueryPort.findCredentialsByTenantIdAndEmail(tenantId, "admin@example.com"))
                .thenReturn(Optional.of(inactiveCredentials));
        when(passwordHashingPort.matches("password123", "$argon2id$hashed")).thenReturn(true);

        assertThrows(AuthenticationFailedException.class, () -> service.login(command));

        verifyNoInteractions(tokenGenerationPort);
    }

    @Test
    void login_wrongPassword() {
        when(tenantQueryPort.findIdBySlug("my-salon")).thenReturn(Optional.of(tenantId));
        when(adminQueryPort.findCredentialsByTenantIdAndEmail(tenantId, "admin@example.com"))
                .thenReturn(Optional.of(activeCredentials));
        when(passwordHashingPort.matches("password123", "$argon2id$hashed")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> service.login(command));

        verifyNoInteractions(tokenGenerationPort);
    }
}

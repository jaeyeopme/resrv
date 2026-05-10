package io.resrv.application.auth;

import io.resrv.application.admin.out.AdminQueryPort;
import io.resrv.application.auth.in.LoginCommand;
import io.resrv.application.auth.in.LoginUseCase;
import io.resrv.application.auth.out.TokenGenerationPort;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LoginService implements LoginUseCase {

    private final TenantQueryPort tenantQueryPort;
    private final AdminQueryPort adminQueryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final TokenGenerationPort tokenGenerationPort;
    private final String dummyHash;

    LoginService(
            final TenantQueryPort tenantQueryPort,
            final AdminQueryPort adminQueryPort,
            final PasswordHashingPort passwordHashingPort,
            final TokenGenerationPort tokenGenerationPort) {
        this.tenantQueryPort = tenantQueryPort;
        this.adminQueryPort = adminQueryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.tokenGenerationPort = tokenGenerationPort;
        this.dummyHash = passwordHashingPort.hash("constant-time-dummy");
    }

    @Override
    public LoginResult login(final LoginCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new AuthenticationFailedException();
        }
        return doLogin(command);
    }

    private LoginResult doLogin(final LoginCommand command) {
        final var tenantId = tenantQueryPort.findIdBySlug(command.tenantSlug());

        if (tenantId.isEmpty()) {
            passwordHashingPort.matches(command.password(), dummyHash);
            throw new AuthenticationFailedException();
        }

        final var credentials =
                adminQueryPort.findCredentialsByTenantIdAndEmail(tenantId.get(), command.email());

        final var hashedPassword =
                credentials.map(UserCredentials::hashedPassword).orElse(dummyHash);

        if (!passwordHashingPort.matches(command.password(), hashedPassword)) {
            throw new AuthenticationFailedException();
        }

        final var verified =
                credentials
                        .filter(UserCredentials::active)
                        .orElseThrow(AuthenticationFailedException::new);

        final var result =
                tokenGenerationPort.generate(
                        verified.userId(), verified.tenantId(), verified.role());
        return new LoginResult(result.accessToken(), result.expiresIn());
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

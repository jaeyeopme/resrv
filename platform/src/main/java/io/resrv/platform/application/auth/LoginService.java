package io.resrv.platform.application.auth;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.in.LoginUseCase;
import io.resrv.platform.application.auth.out.TokenGenerationPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LoginService implements LoginUseCase {

    private final AccountQueryPort accountQueryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final TokenGenerationPort tokenGenerationPort;
    private final String dummyHash;

    public LoginService(
            final AccountQueryPort accountQueryPort,
            final PasswordHashingPort passwordHashingPort,
            final TokenGenerationPort tokenGenerationPort) {
        this.accountQueryPort = accountQueryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.tokenGenerationPort = tokenGenerationPort;
        this.dummyHash = passwordHashingPort.hash("constant-time-dummy");
    }

    @Override
    public LoginResult login(final LoginCommand command) {
        if (command == null || isBlank(command.email()) || isBlank(command.password())) {
            passwordHashingPort.matches(rawPasswordOrEmpty(command), dummyHash);
            throw new AuthenticationFailedException();
        }

        final var account =
                accountQueryPort.findByEmail(accountEmail(command.email(), command.password()));
        final var hashedPassword = account.map(Account::hashedPassword).orElse(dummyHash);
        final var passwordMatches = passwordHashingPort.matches(command.password(), hashedPassword);
        final var activeAccount =
                account.filter(Account::active).orElseThrow(AuthenticationFailedException::new);
        if (!passwordMatches) {
            throw new AuthenticationFailedException();
        }

        return tokenGenerationPort.generate(activeAccount.id());
    }

    private AccountEmail accountEmail(final String value, final String rawPassword) {
        try {
            return new AccountEmail(value);
        } catch (final IllegalArgumentException exception) {
            passwordHashingPort.matches(rawPassword, dummyHash);
            throw new AuthenticationFailedException();
        }
    }

    private static String rawPasswordOrEmpty(final LoginCommand command) {
        if (command == null || command.password() == null) {
            return "";
        }
        return command.password();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

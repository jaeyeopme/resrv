package io.resrv.platform.application.account;

import io.resrv.platform.application.account.in.RegisterAccountCommand;
import io.resrv.platform.application.account.in.RegisterAccountResult;
import io.resrv.platform.application.account.in.RegisterAccountUseCase;
import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.platform.domain.account.AccountName;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterAccountService implements RegisterAccountUseCase {

    private final AccountCommandPort commandPort;
    private final AccountQueryPort queryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final Clock clock;

    public RegisterAccountService(
            final AccountCommandPort commandPort,
            final AccountQueryPort queryPort,
            final PasswordHashingPort passwordHashingPort,
            final Clock clock) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.clock = clock;
    }

    @Override
    public RegisterAccountResult register(final RegisterAccountCommand command) {
        final var email = new AccountEmail(command.email());
        if (queryPort.findByEmail(email).isPresent()) {
            throw new AccountEmailAlreadyExistsException(email);
        }
        final var account =
                Account.create(
                        email,
                        new AccountName(command.name()),
                        passwordHashingPort.hash(command.password()),
                        clock.instant());
        commandPort.save(account);
        return RegisterAccountResult.from(account);
    }
}

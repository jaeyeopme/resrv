package io.resrv.platform.application.auth;

import io.resrv.platform.application.account.out.AccountPasswordCommandPort;
import io.resrv.platform.application.auth.in.ResetPasswordCommand;
import io.resrv.platform.application.auth.in.ResetPasswordResult;
import io.resrv.platform.application.auth.in.ResetPasswordUseCase;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.application.security.out.PasswordResetTokenHashingPort;
import io.resrv.platform.domain.account.PasswordResetToken;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService implements ResetPasswordUseCase {

    private final SignInProtectionQueryPort protectionQueryPort;
    private final SignInProtectionCommandPort protectionCommandPort;
    private final AccountPasswordCommandPort accountPasswordCommandPort;
    private final PasswordHashingPort passwordHashingPort;
    private final PasswordResetTokenHashingPort passwordResetTokenHashingPort;
    private final Clock clock;

    public PasswordResetService(
            final SignInProtectionQueryPort protectionQueryPort,
            final SignInProtectionCommandPort protectionCommandPort,
            final AccountPasswordCommandPort accountPasswordCommandPort,
            final PasswordHashingPort passwordHashingPort,
            final PasswordResetTokenHashingPort passwordResetTokenHashingPort,
            final Clock clock) {
        this.protectionQueryPort = protectionQueryPort;
        this.protectionCommandPort = protectionCommandPort;
        this.accountPasswordCommandPort = accountPasswordCommandPort;
        this.passwordHashingPort = passwordHashingPort;
        this.passwordResetTokenHashingPort = passwordResetTokenHashingPort;
        this.clock = clock;
    }

    @Override
    public ResetPasswordResult resetPassword(final ResetPasswordCommand command) {
        final var now = clock.instant();
        final var digest =
                passwordResetTokenHashingPort.digest(new PasswordResetToken(command.token()));
        final var challenge =
                protectionQueryPort
                        .findActivePasswordResetChallengeByDigest(digest, now)
                        .orElseThrow(PasswordResetTokenInvalidException::new);

        accountPasswordCommandPort.updatePasswordHash(
                challenge.accountId(), passwordHashingPort.hash(command.newPassword()));
        protectionCommandPort.markPasswordResetChallengeUsed(challenge, now);
        protectionCommandPort.clearProtection(challenge.accountId(), now);
        return ResetPasswordResult.success();
    }
}

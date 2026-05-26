package io.resrv.platform.application.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountPasswordCommandPort;
import io.resrv.platform.application.auth.in.ResetPasswordCommand;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.application.security.out.PasswordResetTokenHashingPort;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.platform.domain.account.PasswordResetToken;
import io.resrv.shared.kernel.AccountId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");
    private static final AccountId ACCOUNT_ID = AccountId.create();

    private SignInProtectionQueryPort protectionQueryPort;
    private SignInProtectionCommandPort protectionCommandPort;
    private AccountPasswordCommandPort accountPasswordCommandPort;
    private PasswordHashingPort passwordHashingPort;
    private PasswordResetTokenHashingPort passwordResetTokenHashingPort;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        protectionQueryPort = mock(SignInProtectionQueryPort.class);
        protectionCommandPort = mock(SignInProtectionCommandPort.class);
        accountPasswordCommandPort = mock(AccountPasswordCommandPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        passwordResetTokenHashingPort = mock(PasswordResetTokenHashingPort.class);
        service =
                new PasswordResetService(
                        protectionQueryPort,
                        protectionCommandPort,
                        accountPasswordCommandPort,
                        passwordHashingPort,
                        passwordResetTokenHashingPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void resetPasswordUpdatesHashConsumesChallengeAndClearsProtection() {
        final var challenge =
                PasswordResetChallenge.create(
                        ACCOUNT_ID, "token-digest", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(passwordResetTokenHashingPort.digest(new PasswordResetToken("token")))
                .thenReturn("token-digest");
        when(protectionQueryPort.findActivePasswordResetChallengeByDigest("token-digest", NOW))
                .thenReturn(Optional.of(challenge));
        when(passwordHashingPort.hash("new-passw0rd!")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordCommand("token", "new-passw0rd!"));

        verify(accountPasswordCommandPort).updatePasswordHash(ACCOUNT_ID, "new-hash");
        verify(protectionCommandPort).markPasswordResetChallengeUsed(challenge, NOW);
        verify(protectionCommandPort).clearProtection(ACCOUNT_ID, NOW);
    }

    @Test
    void invalidTokenIsRejected() {
        when(passwordResetTokenHashingPort.digest(new PasswordResetToken("bad-token")))
                .thenReturn("bad-digest");
        when(protectionQueryPort.findActivePasswordResetChallengeByDigest("bad-digest", NOW))
                .thenReturn(Optional.empty());

        assertThrows(
                PasswordResetTokenInvalidException.class,
                () ->
                        service.resetPassword(
                                new ResetPasswordCommand("bad-token", "new-passw0rd!")));
    }
}

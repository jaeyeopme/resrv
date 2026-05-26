package io.resrv.platform.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.out.PasswordResetEmailPort;
import io.resrv.platform.application.auth.out.SignInAttemptCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.application.auth.out.TokenGenerationPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.application.security.out.PasswordResetTokenGeneratorPort;
import io.resrv.platform.application.security.out.PasswordResetTokenHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.account.AccountSignInProtection;
import io.resrv.platform.domain.account.AccountStatus;
import io.resrv.platform.domain.account.PasswordResetToken;
import io.resrv.platform.domain.account.SignInAttemptOutcome;
import io.resrv.shared.kernel.AccountId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String DUMMY_HASH = "$argon2id$dummy";
    private static final String HASHED_PASSWORD = "$argon2id$hashed";

    private AccountQueryPort accountQueryPort;
    private PasswordHashingPort passwordHashingPort;
    private TokenGenerationPort tokenGenerationPort;
    private SignInAttemptCommandPort signInAttemptCommandPort;
    private SignInProtectionCommandPort signInProtectionCommandPort;
    private SignInProtectionQueryPort signInProtectionQueryPort;
    private PasswordResetEmailPort passwordResetEmailPort;
    private PasswordResetTokenGeneratorPort passwordResetTokenGeneratorPort;
    private PasswordResetTokenHashingPort passwordResetTokenHashingPort;
    private LoginService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = mock(AccountQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        tokenGenerationPort = mock(TokenGenerationPort.class);
        signInAttemptCommandPort = mock(SignInAttemptCommandPort.class);
        signInProtectionCommandPort = mock(SignInProtectionCommandPort.class);
        signInProtectionQueryPort = mock(SignInProtectionQueryPort.class);
        passwordResetEmailPort = mock(PasswordResetEmailPort.class);
        passwordResetTokenGeneratorPort = mock(PasswordResetTokenGeneratorPort.class);
        passwordResetTokenHashingPort = mock(PasswordResetTokenHashingPort.class);
        when(passwordHashingPort.hash("constant-time-dummy")).thenReturn(DUMMY_HASH);
        service =
                new LoginService(
                        accountQueryPort,
                        passwordHashingPort,
                        tokenGenerationPort,
                        signInAttemptCommandPort,
                        signInProtectionCommandPort,
                        signInProtectionQueryPort,
                        passwordResetEmailPort,
                        passwordResetTokenGeneratorPort,
                        passwordResetTokenHashingPort,
                        Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                        "https://example.com",
                        Duration.ofMinutes(30));
    }

    @Test
    void activeAccountWithMatchingPasswordGetsToken() {
        final var account = activeAccount("owner@example.com");
        final var result = new LoginResult("access-token", 3600);
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("plain-password", HASHED_PASSWORD)).thenReturn(true);
        when(signInProtectionQueryPort.requiresPasswordReset(account.id())).thenReturn(false);
        when(tokenGenerationPort.generate(account.id())).thenReturn(result);

        final var actual = service.login(new LoginCommand(" Owner@Example.COM ", "plain-password"));

        assertSame(result, actual);
        verify(tokenGenerationPort).generate(account.id());
        verify(signInProtectionCommandPort).clearProtection(account.id(), CREATED_AT);
        verify(signInAttemptCommandPort)
                .recordAttempt(
                        eq(Optional.of(account.id())),
                        any(),
                        eq("unknown"),
                        eq(SignInAttemptOutcome.SUCCESS),
                        eq(CREATED_AT));
    }

    @Test
    void missingAccountChecksDummyHashThenFailsAuthentication() {
        when(accountQueryPort.findByEmail(new AccountEmail("missing@example.com")))
                .thenReturn(Optional.empty());
        when(passwordHashingPort.matches("plain-password", DUMMY_HASH)).thenReturn(false);

        final var exception =
                assertThrows(
                        AuthenticationFailedException.class,
                        () ->
                                service.login(
                                        new LoginCommand("missing@example.com", "plain-password")));

        assertEquals(AuthenticationFailedException.MESSAGE, exception.getMessage());
        verify(passwordHashingPort).matches("plain-password", DUMMY_HASH);
        verify(signInAttemptCommandPort)
                .recordAttempt(
                        eq(Optional.empty()),
                        any(),
                        eq("unknown"),
                        eq(SignInAttemptOutcome.FAILED_UNKNOWN_ACCOUNT),
                        eq(CREATED_AT));
        verify(tokenGenerationPort, never()).generate(any());
    }

    @Test
    void wrongPasswordFailsAuthentication() {
        final var account = activeAccount("owner@example.com");
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("wrong-password", HASHED_PASSWORD)).thenReturn(false);
        when(signInProtectionCommandPort.recordFailedPasswordAttempt(account.id(), CREATED_AT))
                .thenReturn(
                        new AccountSignInProtection(
                                account.id(), 1, false, null, CREATED_AT, CREATED_AT));

        final var exception =
                assertThrows(
                        AuthenticationFailedException.class,
                        () ->
                                service.login(
                                        new LoginCommand("owner@example.com", "wrong-password")));

        assertEquals(AuthenticationFailedException.MESSAGE, exception.getMessage());
        verify(signInProtectionCommandPort).recordFailedPasswordAttempt(account.id(), CREATED_AT);
        verify(tokenGenerationPort, never()).generate(any());
    }

    @Test
    void fifthWrongPasswordSendsPasswordResetEmail() {
        final var account = activeAccount("owner@example.com");
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("wrong-password", HASHED_PASSWORD)).thenReturn(false);
        when(signInProtectionCommandPort.recordFailedPasswordAttempt(account.id(), CREATED_AT))
                .thenReturn(
                        new AccountSignInProtection(
                                account.id(), 5, true, CREATED_AT, CREATED_AT, CREATED_AT));
        when(passwordResetTokenGeneratorPort.generate())
                .thenReturn(new PasswordResetToken("token"));
        when(passwordResetTokenHashingPort.digest(new PasswordResetToken("token")))
                .thenReturn("token-digest");

        assertThrows(
                AuthenticationFailedException.class,
                () -> service.login(new LoginCommand("owner@example.com", "wrong-password")));

        verify(signInProtectionCommandPort)
                .replaceActivePasswordResetChallenges(account.id(), CREATED_AT);
        verify(signInProtectionCommandPort).createPasswordResetChallenge(any());
        verify(passwordResetEmailPort)
                .sendPasswordResetEmail(
                        eq(account.email()),
                        eq("https://example.com/reset-password?token=token"),
                        eq(CREATED_AT.plusSeconds(1800)));
    }

    @Test
    void unrelatedAccountCanStillSignInWhenAnotherAccountRequiresReset() {
        final var unrelatedAccount = activeAccount("member@example.com");
        final var result = new LoginResult("member-token", 3600);
        when(accountQueryPort.findByEmail(new AccountEmail("member@example.com")))
                .thenReturn(Optional.of(unrelatedAccount));
        when(passwordHashingPort.matches("plain-password", HASHED_PASSWORD)).thenReturn(true);
        when(signInProtectionQueryPort.requiresPasswordReset(unrelatedAccount.id()))
                .thenReturn(false);
        when(tokenGenerationPort.generate(unrelatedAccount.id())).thenReturn(result);

        final var actual = service.login(new LoginCommand("member@example.com", "plain-password"));

        assertSame(result, actual);
        verify(tokenGenerationPort).generate(unrelatedAccount.id());
    }

    @Test
    void disabledAccountFailsAuthenticationEvenWithMatchingPassword() {
        final var account = disabledAccount("owner@example.com");
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("plain-password", HASHED_PASSWORD)).thenReturn(true);
        when(signInProtectionQueryPort.requiresPasswordReset(account.id())).thenReturn(false);

        final var exception =
                assertThrows(
                        AuthenticationFailedException.class,
                        () ->
                                service.login(
                                        new LoginCommand("owner@example.com", "plain-password")));

        assertEquals(AuthenticationFailedException.MESSAGE, exception.getMessage());
        verify(tokenGenerationPort, never()).generate(any());
    }

    @Test
    void blankCredentialsFailAuthenticationBeforeLookup() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.login(new LoginCommand(" ", "plain-password")));
        assertThrows(
                AuthenticationFailedException.class,
                () -> service.login(new LoginCommand("owner@example.com", " ")));
        assertThrows(AuthenticationFailedException.class, () -> service.login(null));

        verifyNoInteractions(accountQueryPort, tokenGenerationPort);
    }

    @Test
    void malformedEmailFailsAuthentication() {
        final var exception =
                assertThrows(
                        AuthenticationFailedException.class,
                        () -> service.login(new LoginCommand("not-email", "plain-password")));

        assertEquals(AuthenticationFailedException.MESSAGE, exception.getMessage());
        verifyNoInteractions(accountQueryPort, tokenGenerationPort);
    }

    private static Account activeAccount(final String email) {
        return Account.reconstitute(
                AccountId.create(),
                new AccountEmail(email),
                new AccountName("Owner"),
                HASHED_PASSWORD,
                AccountStatus.ACTIVE,
                CREATED_AT);
    }

    private static Account disabledAccount(final String email) {
        return Account.reconstitute(
                AccountId.create(),
                new AccountEmail(email),
                new AccountName("Owner"),
                HASHED_PASSWORD,
                AccountStatus.DISABLED,
                CREATED_AT);
    }
}

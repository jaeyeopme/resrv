package io.resrv.platform.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.out.TokenGenerationPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.account.AccountStatus;
import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
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
    private LoginService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = mock(AccountQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        tokenGenerationPort = mock(TokenGenerationPort.class);
        when(passwordHashingPort.hash("constant-time-dummy")).thenReturn(DUMMY_HASH);
        service = new LoginService(accountQueryPort, passwordHashingPort, tokenGenerationPort);
    }

    @Test
    void activeAccountWithMatchingPasswordGetsToken() {
        final var account = activeAccount("owner@example.com");
        final var result = new LoginResult("access-token", 3600);
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("plain-password", HASHED_PASSWORD)).thenReturn(true);
        when(tokenGenerationPort.generate(account.id())).thenReturn(result);

        final var actual = service.login(new LoginCommand(" Owner@Example.COM ", "plain-password"));

        assertSame(result, actual);
        verify(tokenGenerationPort).generate(account.id());
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
        verify(tokenGenerationPort, never()).generate(any());
    }

    @Test
    void wrongPasswordFailsAuthentication() {
        final var account = activeAccount("owner@example.com");
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("wrong-password", HASHED_PASSWORD)).thenReturn(false);

        final var exception =
                assertThrows(
                        AuthenticationFailedException.class,
                        () ->
                                service.login(
                                        new LoginCommand("owner@example.com", "wrong-password")));

        assertEquals(AuthenticationFailedException.MESSAGE, exception.getMessage());
        verify(tokenGenerationPort, never()).generate(any());
    }

    @Test
    void disabledAccountFailsAuthenticationEvenWithMatchingPassword() {
        final var account = disabledAccount("owner@example.com");
        when(accountQueryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("plain-password", HASHED_PASSWORD)).thenReturn(true);

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

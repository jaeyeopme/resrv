package io.resrv.platform.application.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.in.RegisterAccountCommand;
import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.platform.domain.account.AccountName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegisterAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AccountCommandPort commandPort;
    private AccountQueryPort queryPort;
    private PasswordHashingPort passwordHashingPort;
    private RegisterAccountService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(AccountCommandPort.class);
        queryPort = mock(AccountQueryPort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        service =
                new RegisterAccountService(
                        commandPort,
                        queryPort,
                        passwordHashingPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registersAccountWithHashedPasswordReturnsEmailAndSavesAccount() {
        when(queryPort.findByEmail(new AccountEmail("owner@example.com")))
                .thenReturn(Optional.empty());
        when(passwordHashingPort.hash("plain-password")).thenReturn("$argon2id$hashed");

        final var result =
                service.register(
                        new RegisterAccountCommand(
                                " Owner@Example.COM ", " Owner Name ", "plain-password"));

        assertEquals("owner@example.com", result.email());
        assertEquals("Owner Name", result.name());

        final var captor = ArgumentCaptor.forClass(Account.class);
        verify(commandPort).save(captor.capture());
        final var saved = captor.getValue();
        assertEquals("$argon2id$hashed", saved.hashedPassword());
        assertEquals("owner@example.com", saved.email().value());
        assertEquals("Owner Name", saved.name().value());
        assertEquals(NOW, saved.createdAt());
        verify(passwordHashingPort).hash("plain-password");
    }

    @Test
    void duplicateEmailThrowsAccountEmailAlreadyExistsException() {
        final var email = new AccountEmail("owner@example.com");
        final var existing =
                Account.create(email, new AccountName("Owner"), "$argon2id$hashed", NOW);
        when(queryPort.findByEmail(email)).thenReturn(Optional.of(existing));

        final var exception =
                assertThrows(
                        AccountEmailAlreadyExistsException.class,
                        () ->
                                service.register(
                                        new RegisterAccountCommand(
                                                "owner@example.com", "Owner", "plain-password")));

        assertEquals("Account email already exists: owner@example.com", exception.getMessage());
        verify(commandPort, never()).save(any());
    }
}

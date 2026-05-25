package io.resrv.platform.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createStartsActive() {
        final var account =
                Account.create(
                        new AccountEmail("owner@example.com"),
                        new AccountName("Owner"),
                        "$argon2id$hashed",
                        NOW);

        assertNotNull(account.id());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertTrue(account.active());
    }

    @Test
    void emailValueNormalized() {
        final var email = new AccountEmail("  OWNER@Example.COM  ");

        assertEquals("owner@example.com", email.value());
    }

    @Test
    void createdAtPreserved() {
        final var account =
                Account.create(
                        new AccountEmail("owner@example.com"),
                        new AccountName("Owner"),
                        "$argon2id$hashed",
                        NOW);

        assertEquals(NOW, account.createdAt());
    }

    @Test
    void blankHashedPasswordRejected() {
        final var email = new AccountEmail("owner@example.com");
        final var name = new AccountName("Owner");

        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> Account.create(email, name, " ", NOW));

        assertEquals("Account hashed password must not be blank", exception.getMessage());
    }

    @Test
    void reconstitutePreservesValuesAndDisabledIsNotActive() {
        final var id = AccountId.create();

        final var account =
                Account.reconstitute(
                        id,
                        new AccountEmail("owner@example.com"),
                        new AccountName("Owner"),
                        "$argon2id$hashed",
                        AccountStatus.DISABLED,
                        NOW);

        assertEquals(id, account.id());
        assertEquals("owner@example.com", account.email().value());
        assertEquals("Owner", account.name().value());
        assertEquals("$argon2id$hashed", account.hashedPassword());
        assertEquals(AccountStatus.DISABLED, account.status());
        assertEquals(NOW, account.createdAt());
        assertFalse(account.active());
    }

    @Test
    void invalidEmailRejected() {
        final var nullException =
                assertThrows(IllegalArgumentException.class, () -> new AccountEmail(null));
        final var malformedException =
                assertThrows(IllegalArgumentException.class, () -> new AccountEmail("not-email"));

        assertEquals("Account email must be valid", nullException.getMessage());
        assertEquals("Account email must be valid", malformedException.getMessage());
    }

    @Test
    void nameValueTrimmed() {
        final var name = new AccountName("  Owner Name  ");

        assertEquals("Owner Name", name.value());
    }

    @Test
    void invalidNameRejected() {
        final var nullException =
                assertThrows(IllegalArgumentException.class, () -> new AccountName(null));
        final var tooLongException =
                assertThrows(
                        IllegalArgumentException.class, () -> new AccountName("x".repeat(101)));

        assertEquals("Account name must be 1-100 characters", nullException.getMessage());
        assertEquals("Account name must be 1-100 characters", tooLongException.getMessage());
    }

    @Test
    void duplicateEmailExceptionContainsNormalizedEmail() {
        final var exception =
                new AccountEmailAlreadyExistsException(new AccountEmail("OWNER@Example.COM"));

        assertEquals("Account email already exists: owner@example.com", exception.getMessage());
    }
}

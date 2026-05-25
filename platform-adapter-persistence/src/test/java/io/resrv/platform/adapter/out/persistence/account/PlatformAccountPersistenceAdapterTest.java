package io.resrv.platform.adapter.out.persistence.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.platform.domain.account.AccountName;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountPersistenceAdapter.class)
class PlatformAccountPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HASHED_PASSWORD = "$argon2id$v=19$m=65536,t=3,p=1$platform";

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private AccountCommandPort commandPort;

    @Autowired private AccountQueryPort queryPort;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindByIdAndEmail() {
        final var account = createAccount("Owner@Example.com", "Owner");

        commandPort.save(account);

        final var foundById = queryPort.findById(account.id()).orElseThrow();
        assertEquals(account.id(), foundById.id());
        assertEquals("owner@example.com", foundById.email().value());
        assertEquals("Owner", foundById.name().value());
        assertEquals(HASHED_PASSWORD, foundById.hashedPassword());
        assertTrue(foundById.active());

        final var foundByEmail = queryPort.findByEmail(new AccountEmail("owner@example.com"));
        assertTrue(foundByEmail.isPresent());
        assertEquals(account.id(), foundByEmail.orElseThrow().id());
    }

    @Test
    void duplicateEmail_throwsAccountEmailAlreadyExistsException() {
        commandPort.save(createAccount("owner@example.com", "Owner One"));

        assertThrows(
                AccountEmailAlreadyExistsException.class,
                () -> commandPort.save(createAccount("owner@example.com", "Owner Two")));
    }

    @Test
    void blankHashedPassword_isRejectedByDatabaseConstraint() {
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        jdbcTemplate.update(
                                """
                                INSERT INTO platform.account
                                    (id, email, name, hashed_password, status, created_at)
                                VALUES (?, ?, ?, ?, ?, ?)
                                """,
                                UUID.randomUUID(),
                                "blank-password@example.com",
                                "Blank Password",
                                " ",
                                "ACTIVE",
                                Timestamp.from(NOW)));
    }

    @Test
    void missingAccount_returnsEmpty() {
        assertFalse(queryPort.findByEmail(new AccountEmail("missing@example.com")).isPresent());
    }

    private static Account createAccount(final String email, final String name) {
        return Account.create(new AccountEmail(email), new AccountName(name), HASHED_PASSWORD, NOW);
    }
}

package io.resrv.platform.adapter.out.persistence.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.application.auth.out.SignInAttemptCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.platform.domain.account.SignInAttemptOutcome;
import io.resrv.shared.kernel.AccountId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountSecurityPersistenceAdapter.class)
class AccountSecurityPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");
    private static final AccountId ACCOUNT_ID =
            AccountId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private SignInAttemptCommandPort attemptCommandPort;

    @Autowired private SignInProtectionCommandPort protectionCommandPort;

    @Autowired private SignInProtectionQueryPort protectionQueryPort;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void recordsKnownAndUnknownSignInAttemptsWithoutRawEmail() {
        insertAccount();

        attemptCommandPort.recordAttempt(
                Optional.of(ACCOUNT_ID),
                "email-digest",
                "caller",
                SignInAttemptOutcome.FAILED_BAD_PASSWORD,
                NOW);
        attemptCommandPort.recordAttempt(
                Optional.empty(),
                "unknown-digest",
                "caller",
                SignInAttemptOutcome.FAILED_UNKNOWN_ACCOUNT,
                NOW);

        final var count =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM platform.sign_in_attempt", Integer.class);
        assertEquals(2, count);
        final var rawEmailCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM platform.sign_in_attempt
                        WHERE email_hash = 'owner@example.com'
                        """,
                        Integer.class);
        assertEquals(0, rawEmailCount);
    }

    @Test
    void failedAttemptsRequirePasswordResetOnFifthFailureAndCanBeCleared() {
        insertAccount();

        for (var attempt = 1; attempt <= 5; attempt++) {
            protectionCommandPort.recordFailedPasswordAttempt(ACCOUNT_ID, NOW.plusSeconds(attempt));
        }

        final var protection = protectionQueryPort.findProtection(ACCOUNT_ID).orElseThrow();
        assertEquals(5, protection.failedPasswordAttempts());
        assertTrue(protection.passwordResetRequired());
        assertTrue(protectionQueryPort.requiresPasswordReset(ACCOUNT_ID));

        protectionCommandPort.clearProtection(ACCOUNT_ID, NOW.plusSeconds(10));

        final var cleared = protectionQueryPort.findProtection(ACCOUNT_ID).orElseThrow();
        assertEquals(0, cleared.failedPasswordAttempts());
        assertFalse(cleared.passwordResetRequired());
    }

    @Test
    void passwordResetChallengeCanBeCreatedReplacedAndConsumed() {
        insertAccount();
        final var challenge =
                PasswordResetChallenge.create(
                        ACCOUNT_ID, "token-digest", NOW, NOW.plusSeconds(3600));

        protectionCommandPort.createPasswordResetChallenge(challenge);

        assertTrue(
                protectionQueryPort
                        .findActivePasswordResetChallengeByDigest(
                                "token-digest", NOW.plusSeconds(10))
                        .isPresent());

        protectionCommandPort.replaceActivePasswordResetChallenges(ACCOUNT_ID, NOW.plusSeconds(20));

        assertFalse(
                protectionQueryPort
                        .findActivePasswordResetChallengeByDigest(
                                "token-digest", NOW.plusSeconds(30))
                        .isPresent());

        final var replacement =
                PasswordResetChallenge.create(
                        ACCOUNT_ID,
                        "replacement-digest",
                        NOW.plusSeconds(30),
                        NOW.plusSeconds(3630));
        final var created = protectionCommandPort.createPasswordResetChallenge(replacement);

        protectionCommandPort.markPasswordResetChallengeUsed(created, NOW.plusSeconds(40));

        assertFalse(
                protectionQueryPort
                        .findActivePasswordResetChallengeByDigest(
                                "replacement-digest", NOW.plusSeconds(50))
                        .isPresent());
    }

    private void insertAccount() {
        jdbcTemplate.update(
                """
                INSERT INTO platform.account (
                    id, email, name, hashed_password, status, created_at
                ) VALUES (?, 'owner@example.com', 'Owner', '$argon2id$test', 'ACTIVE', ?)
                ON CONFLICT (id) DO NOTHING
                """,
                ACCOUNT_ID.value(),
                Timestamp.from(NOW));
    }
}

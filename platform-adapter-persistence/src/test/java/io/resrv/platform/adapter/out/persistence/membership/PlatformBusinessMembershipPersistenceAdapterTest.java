package io.resrv.platform.adapter.out.persistence.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import jakarta.persistence.PersistenceException;
import java.sql.Timestamp;
import java.time.Instant;
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
@Import(BusinessMembershipPersistenceAdapter.class)
class PlatformBusinessMembershipPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HASHED_PASSWORD = "$argon2id$v=19$m=65536,t=3,p=1$platform";

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private BusinessMembershipCommandPort commandPort;

    @Autowired private BusinessMembershipQueryPort queryPort;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindActiveByAccountIdAndBusinessId() {
        final var accountId = insertAccountDirectly("member@example.com");
        final var businessId = insertBusinessDirectly("membership-business");
        final var membership = BusinessMembership.owner(accountId, businessId, NOW);

        commandPort.save(membership);

        final var found =
                queryPort.findActiveByAccountIdAndBusinessId(accountId, businessId).orElseThrow();
        assertEquals(membership.id(), found.id());
        assertEquals(accountId, found.accountId());
        assertEquals(businessId, found.businessId());
        assertEquals(membership.role(), found.role());
        assertTrue(found.active());
    }

    @Test
    void missingMembership_returnsEmpty() {
        final var accountId = insertAccountDirectly("missing-member@example.com");
        final var businessId = insertBusinessDirectly("missing-membership-business");

        assertFalse(
                queryPort.findActiveByAccountIdAndBusinessId(accountId, businessId).isPresent());
    }

    @Test
    void duplicateAccountAndBusiness_bubblesPersistenceException() {
        final var accountId = insertAccountDirectly("duplicate-member@example.com");
        final var businessId = insertBusinessDirectly("duplicate-membership-business");
        commandPort.save(BusinessMembership.owner(accountId, businessId, NOW));

        assertThrows(
                PersistenceException.class,
                () -> commandPort.save(BusinessMembership.owner(accountId, businessId, NOW)));
    }

    @Test
    void orphanMembership_isRejectedByDatabaseConstraint() {
        assertThrows(
                PersistenceException.class,
                () ->
                        commandPort.save(
                                BusinessMembership.owner(
                                        AccountId.create(), BusinessId.create(), NOW)));
    }

    private AccountId insertAccountDirectly(final String email) {
        final var id = AccountId.create();
        jdbcTemplate.update(
                """
                INSERT INTO platform.account
                    (id, email, name, hashed_password, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id.value(),
                email,
                "Member",
                HASHED_PASSWORD,
                "ACTIVE",
                Timestamp.from(NOW));
        return id;
    }

    private BusinessId insertBusinessDirectly(final String slug) {
        final var id = BusinessId.create();
        jdbcTemplate.update(
                """
                INSERT INTO platform.business
                    (id, name, slug, timezone, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id.value(),
                "Studio",
                slug,
                "Asia/Seoul",
                "ACTIVE",
                Timestamp.from(NOW));
        return id;
    }
}

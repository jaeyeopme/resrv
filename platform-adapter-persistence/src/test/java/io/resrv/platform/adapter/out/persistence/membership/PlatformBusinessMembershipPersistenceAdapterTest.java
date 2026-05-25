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
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BusinessMembershipPersistenceAdapter.class)
class PlatformBusinessMembershipPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private BusinessMembershipCommandPort commandPort;

    @Autowired private BusinessMembershipQueryPort queryPort;

    @Test
    void saveAndFindActiveByAccountIdAndBusinessId() {
        final var accountId = AccountId.create();
        final var businessId = BusinessId.create();
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
        assertFalse(
                queryPort
                        .findActiveByAccountIdAndBusinessId(AccountId.create(), BusinessId.create())
                        .isPresent());
    }

    @Test
    void duplicateAccountAndBusiness_bubblesPersistenceException() {
        final var accountId = AccountId.create();
        final var businessId = BusinessId.create();
        commandPort.save(BusinessMembership.owner(accountId, businessId, NOW));

        assertThrows(
                ConstraintViolationException.class,
                () -> commandPort.save(BusinessMembership.owner(accountId, businessId, NOW)));
    }
}

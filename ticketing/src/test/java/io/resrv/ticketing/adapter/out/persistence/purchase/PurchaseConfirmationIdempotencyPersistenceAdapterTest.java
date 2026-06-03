package io.resrv.ticketing.adapter.out.persistence.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.purchase.out.PurchaseConfirmationIdempotencyPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyStatus;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.support.TicketingTestFixtures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan("io.resrv.ticketing.adapter.out.persistence")
class PurchaseConfirmationIdempotencyPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TicketEventCommandPort eventCommandPort;

    @Autowired private PurchaseConfirmationIdempotencyPort idempotencyPort;

    @Test
    void createsReloadsScopesExpiresAndRetainsCleanupFacts() {
        final var event = event();
        eventCommandPort.save(event);
        final var customerId = AccountId.create();
        final var otherCustomerId = AccountId.create();
        final var seat = TicketSeat.createAvailable(event.id(), "A-1");
        final var pending =
                PurchaseConfirmationIdempotency.pending(
                        PurchaseConfirmationIdempotencyKey.of("purchase-key"),
                        customerId,
                        event.id(),
                        List.of(seat.id()),
                        NOW);

        idempotencyPort.save(
                pending.complete(
                        PurchaseConfirmationIdempotencyStatus.UNAVAILABLE_SEATS, null, NOW));

        final var found =
                idempotencyPort
                        .findForCustomerKey(
                                customerId, PurchaseConfirmationIdempotencyKey.of("purchase-key"))
                        .orElseThrow();

        assertThat(found.status())
                .isEqualTo(PurchaseConfirmationIdempotencyStatus.UNAVAILABLE_SEATS);
        assertThat(found.matches(event.id(), List.of(seat.id()))).isTrue();
        assertThat(found.expiredAt(NOW.plusSeconds(86_400))).isTrue();
        assertThat(found.cleanupEligibleAt()).isEqualTo(found.expiresAt().plusSeconds(2_592_000));
        assertThat(
                        idempotencyPort.findForCustomerKey(
                                otherCustomerId,
                                PurchaseConfirmationIdempotencyKey.of("purchase-key")))
                .isEmpty();
    }

    private static TicketEvent event() {
        return TicketingTestFixtures.event("Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
    }
}

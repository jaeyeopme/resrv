package io.resrv.ticketing.adapter.out.persistence.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseCommandPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.application.seat.out.TicketSeatCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
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
class TicketPurchasePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TicketEventCommandPort eventCommandPort;

    @Autowired private TicketSeatCommandPort seatCommandPort;

    @Autowired private TicketSeatQueryPort seatQueryPort;

    @Autowired private TicketPurchaseCommandPort purchaseCommandPort;

    @Autowired private TicketPurchaseQueryPort purchaseQueryPort;

    @Test
    void savesSeatsPurchaseAndReloadsStatusAndOwnership() {
        final var event = event();
        eventCommandPort.save(event);
        final var firstSeat = TicketSeat.createAvailable(event.id(), "A-1");
        final var secondSeat = TicketSeat.createAvailable(event.id(), "A-2");
        seatCommandPort.saveAll(List.of(firstSeat, secondSeat));
        final var customerId = AccountId.create();
        final var purchase =
                TicketPurchase.create(
                        event.id(), customerId, List.of(firstSeat.id(), secondSeat.id()), NOW);

        purchaseCommandPort.save(purchase);
        seatCommandPort.saveAll(
                List.of(
                        firstSeat.purchase(purchase.id(), NOW),
                        secondSeat.purchase(purchase.id(), NOW)));

        final var found = purchaseQueryPort.findById(purchase.id()).orElseThrow();
        final var foundSeats = seatQueryPort.findAllByIds(List.of(firstSeat.id(), secondSeat.id()));

        assertThat(found.customerAccountId()).isEqualTo(customerId);
        assertThat(found.seatIds()).containsExactly(firstSeat.id(), secondSeat.id());
        assertThat(foundSeats)
                .allSatisfy(seat -> assertThat(seat.purchaseId()).isEqualTo(purchase.id()));
        assertThat(
                        purchaseQueryPort.findCustomerPurchaseForSeatSelection(
                                event.id(), customerId, List.of(firstSeat.id(), secondSeat.id())))
                .contains(purchase);
    }

    private static TicketEvent event() {
        return TicketingTestFixtures.event("Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
    }
}

package io.resrv.ticketing.adapter.out.persistence.seat;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatClaimPort;
import io.resrv.ticketing.application.seat.out.TicketSeatCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.support.TicketingTestFixtures;
import jakarta.persistence.EntityManager;
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
class TicketSeatClaimPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-03T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TicketEventCommandPort eventCommandPort;

    @Autowired private TicketSeatCommandPort seatCommandPort;

    @Autowired private TicketSeatQueryPort seatQueryPort;

    @Autowired private TicketSeatClaimPort claimPort;

    @Autowired private EntityManager entityManager;

    @Test
    void claimsAllSelectedSeatsOnceAndLeavesLosingClaimUnchanged() {
        final var event = event();
        eventCommandPort.save(event);
        final var firstSeat = TicketSeat.createAvailable(event.id(), "A-1");
        final var secondSeat = TicketSeat.createAvailable(event.id(), "A-2");
        seatCommandPort.saveAll(List.of(firstSeat, secondSeat));
        final var winningPurchase =
                TicketPurchase.create(
                        event.id(),
                        AccountId.create(),
                        List.of(secondSeat.id(), firstSeat.id()),
                        NOW);
        final var losingPurchase =
                TicketPurchase.create(event.id(), AccountId.create(), List.of(firstSeat.id()), NOW);
        entityManager.flush();

        assertThat(claimPort.claimAvailableSeats(winningPurchase)).isTrue();
        assertThat(claimPort.claimAvailableSeats(losingPurchase)).isFalse();
        entityManager.clear();

        final var foundSeats = seatQueryPort.findAllByIds(List.of(firstSeat.id(), secondSeat.id()));
        assertThat(foundSeats)
                .allSatisfy(seat -> assertThat(seat.purchaseId()).isEqualTo(winningPurchase.id()));
    }

    private static TicketEvent event() {
        return TicketingTestFixtures.event("Concert", Instant.parse("2026-06-03T01:00:00Z"), NOW);
    }
}

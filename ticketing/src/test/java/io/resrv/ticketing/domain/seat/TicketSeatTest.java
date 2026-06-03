package io.resrv.ticketing.domain.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class TicketSeatTest {

    @Test
    void availableSeatBelongsToEventAndCanBecomePurchased() {
        final var eventId = TicketEventId.create();
        final var purchaseId = TicketPurchaseId.create();
        final var purchasedAt = Instant.parse("2026-06-03T00:00:00Z");
        final var seat = TicketSeat.createAvailable(eventId, "A-1");

        final var purchased = seat.purchase(purchaseId, purchasedAt);

        assertThat(seat.isAvailableFor(eventId)).isTrue();
        assertThat(seat.displayLabel()).isEqualTo("A-1");
        assertThat(purchased.status()).isEqualTo(TicketSeatStatus.PURCHASED);
        assertThat(purchased.purchasedAt()).isEqualTo(purchasedAt);
        assertThat(purchased.purchaseId()).isEqualTo(purchaseId);
    }

    @Test
    void purchasedSeatCannotBePurchasedAgain() {
        final var seat =
                TicketSeat.createAvailable(TicketEventId.create(), "A-1")
                        .purchase(TicketPurchaseId.create(), Instant.parse("2026-06-03T00:00:00Z"));

        assertThatThrownBy(
                        () ->
                                seat.purchase(
                                        TicketPurchaseId.create(),
                                        Instant.parse("2026-06-03T00:01:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket seat is not available");
    }
}

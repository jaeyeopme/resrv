package io.resrv.ticketing.domain.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TicketPurchaseTest {

    @Test
    void purchaseOwnsSelectedSeatsForCustomer() {
        final var customerId = AccountId.create();
        final var seats = List.of(TicketSeatId.create(), TicketSeatId.create());
        final var purchase =
                TicketPurchase.create(
                        TicketEventId.create(),
                        customerId,
                        seats,
                        Instant.parse("2026-06-03T00:00:00Z"));

        assertThat(purchase.ownsSameSelection(customerId, seats)).isTrue();
        assertThat(purchase.ownsSameSelection(AccountId.create(), seats)).isFalse();
    }

    @Test
    void duplicateSelectedSeatsAreRejected() {
        final var seatId = TicketSeatId.create();

        assertThatThrownBy(
                        () ->
                                TicketPurchase.create(
                                        TicketEventId.create(),
                                        AccountId.create(),
                                        List.of(seatId, seatId),
                                        Instant.parse("2026-06-03T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket purchase cannot contain duplicate seats");
    }
}

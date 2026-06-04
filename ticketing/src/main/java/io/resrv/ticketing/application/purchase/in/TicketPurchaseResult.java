package io.resrv.ticketing.application.purchase.in;

import io.resrv.ticketing.domain.purchase.TicketPurchase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketPurchaseResult(
        Outcome outcome,
        UUID id,
        UUID ticketEventId,
        UUID customerAccountId,
        List<UUID> seatIds,
        Instant confirmedAt) {

    public enum Outcome {
        PURCHASED,
        UNAVAILABLE_SEATS
    }

    public static TicketPurchaseResult from(final TicketPurchase purchase) {
        return new TicketPurchaseResult(
                Outcome.PURCHASED,
                purchase.id().value(),
                purchase.ticketEventId().value(),
                purchase.customerAccountId().value(),
                purchase.seatIds().stream().map(seatId -> seatId.value()).toList(),
                purchase.confirmedAt());
    }

    public static TicketPurchaseResult unavailable(
            final UUID ticketEventId, final UUID customerAccountId, final List<UUID> seatIds) {
        return new TicketPurchaseResult(
                Outcome.UNAVAILABLE_SEATS, null, ticketEventId, customerAccountId, seatIds, null);
    }

    public boolean purchased() {
        return outcome == Outcome.PURCHASED;
    }
}

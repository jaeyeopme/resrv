package io.resrv.ticketing.application.purchase.in;

import io.resrv.ticketing.domain.purchase.TicketPurchase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketPurchaseResult(
        UUID id,
        UUID ticketEventId,
        UUID customerAccountId,
        List<UUID> seatIds,
        Instant confirmedAt) {

    public static TicketPurchaseResult from(final TicketPurchase purchase) {
        return new TicketPurchaseResult(
                purchase.id().value(),
                purchase.ticketEventId().value(),
                purchase.customerAccountId().value(),
                purchase.seatIds().stream().map(seatId -> seatId.value()).toList(),
                purchase.confirmedAt());
    }
}

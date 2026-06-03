package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.purchase.in.TicketPurchaseResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record TicketPurchaseResponse(
        String outcome,
        UUID id,
        UUID ticketEventId,
        UUID customerAccountId,
        List<UUID> seatIds,
        Instant confirmedAt) {

    static TicketPurchaseResponse from(final TicketPurchaseResult result) {
        return new TicketPurchaseResponse(
                result.outcome().name(),
                result.id(),
                result.ticketEventId(),
                result.customerAccountId(),
                result.seatIds(),
                result.confirmedAt());
    }
}

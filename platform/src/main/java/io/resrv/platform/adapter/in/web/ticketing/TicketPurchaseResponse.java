package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.purchase.in.TicketPurchaseResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record TicketPurchaseResponse(
        @Schema(
                        description = "Purchase outcome",
                        allowableValues = {"PURCHASED", "UNAVAILABLE_SEATS"})
                String outcome,
        @Schema(description = "Confirmed ticket purchase ID") UUID id,
        @Schema(description = "Ticket event ID") UUID ticketEventId,
        @Schema(description = "Purchasing customer account ID") UUID customerAccountId,
        @Schema(description = "Selected ticket seat IDs") List<UUID> seatIds,
        @Schema(description = "Purchase confirmation time") Instant confirmedAt) {

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

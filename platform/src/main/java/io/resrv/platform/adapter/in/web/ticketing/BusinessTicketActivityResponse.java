package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.activity.in.BusinessTicketActivityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record BusinessTicketActivityResponse(
        @Schema(description = "Ticket event ID") UUID ticketEventId,
        @Schema(description = "Completed purchases for this ticket event") List<Item> items) {

    static BusinessTicketActivityResponse from(final BusinessTicketActivityResult result) {
        return new BusinessTicketActivityResponse(
                result.ticketEventId(), result.items().stream().map(Item::from).toList());
    }

    @Schema(name = "BusinessTicketActivityResponseItem")
    record Item(
            @Schema(description = "Confirmed ticket purchase ID") UUID purchaseId,
            @Schema(description = "Purchasing customer account ID") UUID customerAccountId,
            @Schema(description = "Purchased seats") List<CustomerTicketHistoryResponse.Seat> seats,
            @Schema(description = "Purchase confirmation time") Instant confirmedAt) {

        static Item from(final BusinessTicketActivityResult.Item item) {
            return new Item(
                    item.purchaseId(),
                    item.customerAccountId(),
                    item.seats().stream()
                            .map(
                                    seat ->
                                            new CustomerTicketHistoryResponse.Seat(
                                                    seat.seatId(), seat.displayLabel()))
                            .toList(),
                    item.confirmedAt());
        }
    }
}

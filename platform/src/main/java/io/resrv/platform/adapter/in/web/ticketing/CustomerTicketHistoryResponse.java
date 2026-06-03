package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CustomerTicketHistoryResponse(List<Item> items) {

    static CustomerTicketHistoryResponse from(final CustomerTicketHistoryResult result) {
        return new CustomerTicketHistoryResponse(result.items().stream().map(Item::from).toList());
    }

    @Schema(name = "CustomerTicketHistoryResponseItem")
    record Item(
            @Schema(description = "Confirmed ticket purchase ID") UUID purchaseId,
            @Schema(description = "Ticket event ID") UUID ticketEventId,
            @Schema(description = "Ticket event title") String eventTitle,
            @Schema(description = "Ticket event start time") Instant eventStartAt,
            @Schema(description = "Purchased seats") List<Seat> seats,
            @Schema(description = "Purchase confirmation time") Instant confirmedAt) {

        static Item from(final CustomerTicketHistoryResult.Item item) {
            return new Item(
                    item.purchaseId(),
                    item.ticketEventId(),
                    item.eventTitle(),
                    item.eventStartAt(),
                    item.seats().stream().map(Seat::from).toList(),
                    item.confirmedAt());
        }
    }

    @Schema(name = "TicketSeatResponse")
    record Seat(
            @Schema(description = "Ticket seat ID") UUID seatId,
            @Schema(description = "Seat display label") String displayLabel) {

        static Seat from(final CustomerTicketHistoryResult.Seat seat) {
            return new Seat(seat.seatId(), seat.displayLabel());
        }
    }
}

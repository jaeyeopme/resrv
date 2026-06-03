package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CustomerTicketHistoryResponse(List<Item> items) {

    static CustomerTicketHistoryResponse from(final CustomerTicketHistoryResult result) {
        return new CustomerTicketHistoryResponse(result.items().stream().map(Item::from).toList());
    }

    record Item(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            Instant eventStartAt,
            List<Seat> seats,
            Instant confirmedAt) {

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

    record Seat(UUID seatId, String displayLabel) {

        static Seat from(final CustomerTicketHistoryResult.Seat seat) {
            return new Seat(seat.seatId(), seat.displayLabel());
        }
    }
}

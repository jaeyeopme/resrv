package io.resrv.ticketing.application.activity.in;

import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerTicketHistoryResult(List<Item> items) {

    public CustomerTicketHistoryResult {
        items = List.copyOf(items);
    }

    public static CustomerTicketHistoryResult from(
            final List<TicketPurchaseActivityQueryPort.CustomerPurchaseView> views) {
        return new CustomerTicketHistoryResult(views.stream().map(Item::from).toList());
    }

    public record Item(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            Instant eventStartAt,
            List<Seat> seats,
            Instant confirmedAt) {

        static Item from(final TicketPurchaseActivityQueryPort.CustomerPurchaseView view) {
            return new Item(
                    view.purchaseId(),
                    view.ticketEventId(),
                    view.eventTitle(),
                    view.eventStartAt(),
                    view.seats().stream().map(Seat::from).toList(),
                    view.confirmedAt());
        }
    }

    public record Seat(UUID seatId, String displayLabel) {

        static Seat from(final TicketPurchaseActivityQueryPort.SeatView seat) {
            return new Seat(seat.seatId(), seat.displayLabel());
        }
    }
}

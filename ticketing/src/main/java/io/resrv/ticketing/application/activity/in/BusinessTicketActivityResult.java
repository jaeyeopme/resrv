package io.resrv.ticketing.application.activity.in;

import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessTicketActivityResult(UUID ticketEventId, List<Item> items) {

    public BusinessTicketActivityResult {
        items = List.copyOf(items);
    }

    public static BusinessTicketActivityResult from(
            final UUID ticketEventId,
            final List<TicketPurchaseActivityQueryPort.BusinessPurchaseView> views) {
        return new BusinessTicketActivityResult(
                ticketEventId, views.stream().map(Item::from).toList());
    }

    public record Item(
            UUID purchaseId,
            UUID customerAccountId,
            List<CustomerTicketHistoryResult.Seat> seats,
            Instant confirmedAt) {

        static Item from(final TicketPurchaseActivityQueryPort.BusinessPurchaseView view) {
            return new Item(
                    view.purchaseId(),
                    view.customerAccountId(),
                    view.seats().stream().map(CustomerTicketHistoryResult.Seat::from).toList(),
                    view.confirmedAt());
        }
    }
}

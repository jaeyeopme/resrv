package io.resrv.platform.adapter.in.web.ticketing;

import io.resrv.ticketing.application.activity.in.BusinessTicketActivityResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record BusinessTicketActivityResponse(UUID ticketEventId, List<Item> items) {

    static BusinessTicketActivityResponse from(final BusinessTicketActivityResult result) {
        return new BusinessTicketActivityResponse(
                result.ticketEventId(), result.items().stream().map(Item::from).toList());
    }

    record Item(
            UUID purchaseId,
            UUID customerAccountId,
            List<CustomerTicketHistoryResponse.Seat> seats,
            Instant confirmedAt) {

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

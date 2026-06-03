package io.resrv.ticketing.application.purchase.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.List;
import java.util.Objects;

public record ConfirmTicketPurchaseCommand(
        TicketEventId ticketEventId, AccountId customerAccountId, List<TicketSeatId> seatIds) {

    public ConfirmTicketPurchaseCommand {
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(customerAccountId, "Customer account id must not be null");
        Objects.requireNonNull(seatIds, "Ticket seat ids must not be null");
        seatIds = List.copyOf(seatIds);
    }
}

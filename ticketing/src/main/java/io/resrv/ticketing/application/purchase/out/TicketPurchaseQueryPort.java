package io.resrv.ticketing.application.purchase.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.List;
import java.util.Optional;

public interface TicketPurchaseQueryPort {

    Optional<TicketPurchase> findById(TicketPurchaseId ticketPurchaseId);

    Optional<TicketPurchase> findCustomerPurchaseForSeatSelection(
            TicketEventId ticketEventId, AccountId customerAccountId, List<TicketSeatId> seatIds);
}

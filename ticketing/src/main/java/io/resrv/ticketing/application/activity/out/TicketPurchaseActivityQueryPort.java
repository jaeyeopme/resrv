package io.resrv.ticketing.application.activity.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TicketPurchaseActivityQueryPort {

    List<CustomerPurchaseView> findCustomerPurchases(AccountId customerAccountId);

    List<BusinessPurchaseView> findBusinessEventPurchases(
            BusinessId businessId, TicketEventId ticketEventId);

    record CustomerPurchaseView(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            Instant eventStartAt,
            List<SeatView> seats,
            Instant confirmedAt) {}

    record BusinessPurchaseView(
            UUID purchaseId,
            UUID ticketEventId,
            String eventTitle,
            UUID customerAccountId,
            List<SeatView> seats,
            Instant confirmedAt) {}

    record SeatView(UUID seatId, String displayLabel) {}
}

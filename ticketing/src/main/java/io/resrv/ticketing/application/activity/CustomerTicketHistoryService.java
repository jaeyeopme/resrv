package io.resrv.ticketing.application.activity;

import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryQuery;
import io.resrv.ticketing.application.activity.in.CustomerTicketHistoryResult;
import io.resrv.ticketing.application.activity.out.TicketPurchaseActivityQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerTicketHistoryService {

    private final TicketPurchaseActivityQueryPort activityQueryPort;

    public CustomerTicketHistoryService(final TicketPurchaseActivityQueryPort activityQueryPort) {
        this.activityQueryPort = activityQueryPort;
    }

    @Transactional(readOnly = true)
    public CustomerTicketHistoryResult list(final CustomerTicketHistoryQuery query) {
        return CustomerTicketHistoryResult.from(
                activityQueryPort.findCustomerPurchases(query.customerAccountId()));
    }
}

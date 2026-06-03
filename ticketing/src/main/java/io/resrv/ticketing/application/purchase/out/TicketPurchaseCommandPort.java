package io.resrv.ticketing.application.purchase.out;

import io.resrv.ticketing.domain.purchase.TicketPurchase;

public interface TicketPurchaseCommandPort {

    void save(TicketPurchase purchase);
}

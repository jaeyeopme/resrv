package io.resrv.ticketing.application.purchase;

public class TicketPurchaseAccessDeniedException extends RuntimeException {

    public TicketPurchaseAccessDeniedException(final String message) {
        super(message);
    }
}

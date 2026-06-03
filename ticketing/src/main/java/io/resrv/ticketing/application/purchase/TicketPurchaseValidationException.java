package io.resrv.ticketing.application.purchase;

public class TicketPurchaseValidationException extends RuntimeException {

    public TicketPurchaseValidationException(final String message) {
        super(message);
    }
}

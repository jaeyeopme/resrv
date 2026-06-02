package io.resrv.ticketing.application;

public class TicketingValidationException extends RuntimeException {

    public TicketingValidationException(final String message) {
        super(message);
    }
}

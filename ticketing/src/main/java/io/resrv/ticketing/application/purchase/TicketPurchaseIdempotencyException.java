package io.resrv.ticketing.application.purchase;

public final class TicketPurchaseIdempotencyException extends RuntimeException {

    public enum Reason {
        INVALID_RETRY,
        EXPIRED_KEY
    }

    private final Reason reason;

    private TicketPurchaseIdempotencyException(final Reason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public static TicketPurchaseIdempotencyException invalidRetry() {
        return new TicketPurchaseIdempotencyException(
                Reason.INVALID_RETRY,
                "Idempotency key was already used with different purchase details");
    }

    public static TicketPurchaseIdempotencyException expiredKey() {
        return new TicketPurchaseIdempotencyException(
                Reason.EXPIRED_KEY, "Idempotency key replay window has expired");
    }

    public Reason reason() {
        return reason;
    }
}

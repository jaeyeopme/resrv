package io.resrv.ticketing.domain.purchase;

public enum PurchaseConfirmationIdempotencyStatus {
    PENDING,
    PURCHASED,
    UNAVAILABLE_SEATS,
    VALIDATION_FAILED
}

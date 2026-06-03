package io.resrv.ticketing.domain.purchase;

import java.util.Objects;

public record PurchaseConfirmationIdempotencyKey(String value) {

    public PurchaseConfirmationIdempotencyKey {
        Objects.requireNonNull(value, "Purchase confirmation idempotency key must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Purchase confirmation idempotency key is required");
        }
        if (value.length() > 120) {
            throw new IllegalArgumentException(
                    "Purchase confirmation idempotency key must be at most 120 characters");
        }
    }

    public static PurchaseConfirmationIdempotencyKey of(final String value) {
        return new PurchaseConfirmationIdempotencyKey(value);
    }
}

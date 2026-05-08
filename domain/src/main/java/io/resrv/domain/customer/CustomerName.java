package io.resrv.domain.customer;

public record CustomerName(String value) {

    private static final int MAX_LENGTH = 100;

    public CustomerName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        final var normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Customer name must be %d characters or fewer".formatted(MAX_LENGTH));
        }
        value = normalized;
    }
}

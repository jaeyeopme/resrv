package io.resrv.ticketing.domain.inventory;

import java.util.Objects;

public record TicketInventoryTier(
        TicketInventoryTierId id,
        String displayName,
        int total,
        int reserved,
        int confirmed,
        int softReserved) {

    private static final int MAX_DISPLAY_NAME_LENGTH = 200;

    public TicketInventoryTier {
        Objects.requireNonNull(id, "Ticket inventory tier id must not be null");
        displayName = normalizeDisplayName(displayName);
        if (total < 0 || reserved < 0 || confirmed < 0 || softReserved < 0) {
            throw new IllegalArgumentException("Ticket inventory counters must not be negative");
        }
        if (reserved + confirmed + softReserved > total) {
            throw new IllegalArgumentException(
                    "Reserved, confirmed, and soft-reserved units must not exceed total");
        }
    }

    public static TicketInventoryTier create(
            final String displayName,
            final int total,
            final int reserved,
            final int confirmed,
            final int softReserved) {
        return new TicketInventoryTier(
                TicketInventoryTierId.create(),
                displayName,
                total,
                reserved,
                confirmed,
                softReserved);
    }

    public int available() {
        return total - reserved - confirmed - softReserved;
    }

    private static String normalizeDisplayName(final String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticket inventory tier display name must not be blank");
        }
        final var trimmed = displayName.strip();
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Ticket inventory tier display name must be 1-200 characters");
        }
        return trimmed;
    }
}

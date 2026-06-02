package io.resrv.ticketing.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record TicketInventoryId(UUID value) {

    public TicketInventoryId {
        Objects.requireNonNull(value, "Ticket inventory id must not be null");
    }

    public static TicketInventoryId create() {
        return of(UUID.randomUUID());
    }

    public static TicketInventoryId of(final UUID value) {
        return new TicketInventoryId(value);
    }
}

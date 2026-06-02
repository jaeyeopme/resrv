package io.resrv.ticketing.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record TicketInventoryTierId(UUID value) {

    public TicketInventoryTierId {
        Objects.requireNonNull(value, "Ticket inventory tier id must not be null");
    }

    public static TicketInventoryTierId create() {
        return of(UUID.randomUUID());
    }

    public static TicketInventoryTierId of(final UUID value) {
        return new TicketInventoryTierId(value);
    }
}

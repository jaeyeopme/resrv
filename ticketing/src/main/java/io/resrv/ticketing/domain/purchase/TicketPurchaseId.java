package io.resrv.ticketing.domain.purchase;

import java.util.Objects;
import java.util.UUID;

public record TicketPurchaseId(UUID value) {

    public TicketPurchaseId {
        Objects.requireNonNull(value, "Ticket purchase id must not be null");
    }

    public static TicketPurchaseId create() {
        return of(UUID.randomUUID());
    }

    public static TicketPurchaseId of(final UUID value) {
        return new TicketPurchaseId(value);
    }
}

package io.resrv.ticketing.domain.event;

import java.util.Objects;
import java.util.UUID;

public record TicketEventId(UUID value) {

    public TicketEventId {
        Objects.requireNonNull(value, "Ticket event id must not be null");
    }

    public static TicketEventId create() {
        return of(UUID.randomUUID());
    }

    public static TicketEventId of(final UUID value) {
        return new TicketEventId(value);
    }
}

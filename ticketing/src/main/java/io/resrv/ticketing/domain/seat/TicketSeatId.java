package io.resrv.ticketing.domain.seat;

import java.util.Objects;
import java.util.UUID;

public record TicketSeatId(UUID value) {

    public TicketSeatId {
        Objects.requireNonNull(value, "Ticket seat id must not be null");
    }

    public static TicketSeatId create() {
        return of(UUID.randomUUID());
    }

    public static TicketSeatId of(final UUID value) {
        return new TicketSeatId(value);
    }
}

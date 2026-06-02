package io.resrv.ticketing.application.event.in;

import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.Objects;

public record GetTicketEventQuery(TicketEventId ticketEventId) {

    public GetTicketEventQuery {
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
    }
}

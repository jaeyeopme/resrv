package io.resrv.ticketing.application.inventory.in;

import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import java.util.Objects;

public record GetTicketInventoryQuery(TicketInventoryId ticketInventoryId) {

    public GetTicketInventoryQuery {
        Objects.requireNonNull(ticketInventoryId, "Ticket inventory id must not be null");
    }
}

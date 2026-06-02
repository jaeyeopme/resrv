package io.resrv.ticketing.application.inventory.in;

import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.List;
import java.util.Objects;

public record CreateTicketInventoryCommand(TicketEventId ticketEventId, List<TierCommand> tiers) {

    public CreateTicketInventoryCommand {
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(tiers, "Ticket inventory tiers must not be null");
        tiers = List.copyOf(tiers);
    }

    public record TierCommand(
            String displayName, int total, int reserved, int confirmed, int softReserved) {}
}

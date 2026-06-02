package io.resrv.ticketing.domain.inventory;

import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TicketInventory(
        TicketInventoryId id,
        TicketEventId ticketEventId,
        List<TicketInventoryTier> tiers,
        Instant createdAt,
        Instant updatedAt) {

    public TicketInventory {
        Objects.requireNonNull(id, "Ticket inventory id must not be null");
        Objects.requireNonNull(ticketEventId, "Ticket event id must not be null");
        Objects.requireNonNull(tiers, "Ticket inventory tiers must not be null");
        tiers = List.copyOf(tiers);
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Ticket inventory must contain at least one tier");
        }
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static TicketInventory create(
            final TicketEvent ticketEvent,
            final List<TicketInventoryTier> tiers,
            final Instant now) {
        Objects.requireNonNull(ticketEvent, "Ticket event must not be null");
        if (!ticketEvent.allowsFutureClaims()) {
            throw new IllegalArgumentException("Inactive ticket events cannot create inventory");
        }
        return new TicketInventory(TicketInventoryId.create(), ticketEvent.id(), tiers, now, now);
    }

    public static TicketInventory reconstitute(
            final TicketInventoryId id,
            final TicketEventId ticketEventId,
            final List<TicketInventoryTier> tiers,
            final Instant createdAt,
            final Instant updatedAt) {
        return new TicketInventory(id, ticketEventId, tiers, createdAt, updatedAt);
    }
}

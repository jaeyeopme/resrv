package io.resrv.ticketing.application.inventory.in;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import io.resrv.ticketing.domain.inventory.TicketInventoryTier;
import io.resrv.ticketing.domain.inventory.TicketInventoryTierId;
import java.util.List;

public record TicketInventoryResult(
        TicketInventoryId id, TicketEventId ticketEventId, List<TierResult> tiers) {

    public TicketInventoryResult {
        tiers = List.copyOf(tiers);
    }

    public static TicketInventoryResult from(final TicketInventory inventory) {
        return new TicketInventoryResult(
                inventory.id(),
                inventory.ticketEventId(),
                inventory.tiers().stream().map(TierResult::from).toList());
    }

    public record TierResult(
            TicketInventoryTierId id,
            String displayName,
            int total,
            int reserved,
            int confirmed,
            int softReserved,
            int available) {

        static TierResult from(final TicketInventoryTier tier) {
            return new TierResult(
                    tier.id(),
                    tier.displayName(),
                    tier.total(),
                    tier.reserved(),
                    tier.confirmed(),
                    tier.softReserved(),
                    tier.available());
        }
    }
}

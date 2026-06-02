package io.resrv.ticketing.adapter.out.persistence.inventory;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import io.resrv.ticketing.domain.inventory.TicketInventoryTier;
import io.resrv.ticketing.domain.inventory.TicketInventoryTierId;

final class TicketInventoryMapper {

    private TicketInventoryMapper() {}

    static TicketInventoryJpaEntity toEntity(final TicketInventory inventory) {
        final var entity =
                new TicketInventoryJpaEntity(
                        inventory.id().value(),
                        inventory.ticketEventId().value(),
                        inventory.createdAt(),
                        inventory.updatedAt());
        entity.replaceTiers(
                inventory.tiers().stream()
                        .map(
                                tier ->
                                        new TicketInventoryTierJpaEntity(
                                                tier.id().value(),
                                                tier.displayName(),
                                                tier.total(),
                                                tier.reserved(),
                                                tier.confirmed(),
                                                tier.softReserved()))
                        .toList());
        return entity;
    }

    static TicketInventory toDomain(final TicketInventoryJpaEntity entity) {
        return TicketInventory.reconstitute(
                TicketInventoryId.of(entity.id()),
                TicketEventId.of(entity.ticketEventId()),
                entity.tiers().stream().map(TicketInventoryMapper::toDomain).toList(),
                entity.createdAt(),
                entity.updatedAt());
    }

    private static TicketInventoryTier toDomain(final TicketInventoryTierJpaEntity entity) {
        return new TicketInventoryTier(
                TicketInventoryTierId.of(entity.id()),
                entity.displayName(),
                entity.total(),
                entity.reserved(),
                entity.confirmed(),
                entity.softReserved());
    }
}

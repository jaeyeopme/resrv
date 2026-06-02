package io.resrv.ticketing.adapter.out.persistence.inventory;

import io.resrv.ticketing.application.inventory.out.TicketInventoryCommandPort;
import io.resrv.ticketing.application.inventory.out.TicketInventoryQueryPort;
import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TicketInventoryPersistenceAdapter
        implements TicketInventoryCommandPort, TicketInventoryQueryPort {

    private final TicketInventoryJpaRepository repository;

    TicketInventoryPersistenceAdapter(final TicketInventoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final TicketInventory inventory) {
        repository.save(TicketInventoryMapper.toEntity(inventory));
    }

    @Override
    public Optional<TicketInventory> findById(final TicketInventoryId ticketInventoryId) {
        return repository.findById(ticketInventoryId.value()).map(TicketInventoryMapper::toDomain);
    }
}

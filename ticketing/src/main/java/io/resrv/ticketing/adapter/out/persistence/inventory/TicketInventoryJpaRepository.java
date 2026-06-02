package io.resrv.ticketing.adapter.out.persistence.inventory;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface TicketInventoryJpaRepository extends CrudRepository<TicketInventoryJpaEntity, UUID> {

    Optional<TicketInventoryJpaEntity> findByTicketEventId(UUID ticketEventId);
}

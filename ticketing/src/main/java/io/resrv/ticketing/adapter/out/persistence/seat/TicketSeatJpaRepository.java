package io.resrv.ticketing.adapter.out.persistence.seat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface TicketSeatJpaRepository extends CrudRepository<TicketSeatJpaEntity, UUID> {

    List<TicketSeatJpaEntity> findAllByIdIn(List<UUID> ids);

    List<TicketSeatJpaEntity> findAllByTicketEventId(UUID ticketEventId);
}

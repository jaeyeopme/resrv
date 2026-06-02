package io.resrv.ticketing.adapter.out.persistence.event;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface TicketEventJpaRepository extends CrudRepository<TicketEventJpaEntity, UUID> {}

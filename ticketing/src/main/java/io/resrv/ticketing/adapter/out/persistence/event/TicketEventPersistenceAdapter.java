package io.resrv.ticketing.adapter.out.persistence.event;

import io.resrv.ticketing.application.event.out.TicketEventCommandPort;
import io.resrv.ticketing.application.event.out.TicketEventQueryPort;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TicketEventPersistenceAdapter implements TicketEventCommandPort, TicketEventQueryPort {

    private final TicketEventJpaRepository repository;

    TicketEventPersistenceAdapter(final TicketEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final TicketEvent event) {
        repository.save(TicketEventMapper.toEntity(event));
    }

    @Override
    public Optional<TicketEvent> findById(final TicketEventId ticketEventId) {
        return repository.findById(ticketEventId.value()).map(TicketEventMapper::toDomain);
    }
}

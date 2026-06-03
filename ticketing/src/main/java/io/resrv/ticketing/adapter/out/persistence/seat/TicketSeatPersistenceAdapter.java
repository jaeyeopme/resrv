package io.resrv.ticketing.adapter.out.persistence.seat;

import io.resrv.ticketing.application.seat.out.TicketSeatCommandPort;
import io.resrv.ticketing.application.seat.out.TicketSeatQueryPort;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TicketSeatPersistenceAdapter implements TicketSeatCommandPort, TicketSeatQueryPort {

    private final TicketSeatJpaRepository repository;

    TicketSeatPersistenceAdapter(final TicketSeatJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final TicketSeat seat) {
        repository.save(TicketSeatMapper.toEntity(seat));
    }

    @Override
    public void saveAll(final List<TicketSeat> seats) {
        repository.saveAll(seats.stream().map(TicketSeatMapper::toEntity).toList());
    }

    @Override
    public Optional<TicketSeat> findById(final TicketSeatId ticketSeatId) {
        return repository.findById(ticketSeatId.value()).map(TicketSeatMapper::toDomain);
    }

    @Override
    public List<TicketSeat> findAllByIds(final List<TicketSeatId> ticketSeatIds) {
        final var ids = ticketSeatIds.stream().map(TicketSeatId::value).toList();
        return repository.findAllByIdIn(ids).stream().map(TicketSeatMapper::toDomain).toList();
    }

    @Override
    public List<TicketSeat> findAllByEventId(final TicketEventId ticketEventId) {
        return repository.findAllByTicketEventId(ticketEventId.value()).stream()
                .map(TicketSeatMapper::toDomain)
                .toList();
    }
}

package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseCommandPort;
import io.resrv.ticketing.application.purchase.out.TicketPurchaseQueryPort;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class TicketPurchasePersistenceAdapter
        implements TicketPurchaseCommandPort, TicketPurchaseQueryPort {

    private final TicketPurchaseJpaRepository repository;

    TicketPurchasePersistenceAdapter(final TicketPurchaseJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final TicketPurchase purchase) {
        repository.save(TicketPurchaseMapper.toEntity(purchase));
    }

    @Override
    public Optional<TicketPurchase> findById(final TicketPurchaseId ticketPurchaseId) {
        return repository.findById(ticketPurchaseId.value()).map(TicketPurchaseMapper::toDomain);
    }

    @Override
    public Optional<TicketPurchase> findCustomerPurchaseForSeatSelection(
            final TicketEventId ticketEventId,
            final AccountId customerAccountId,
            final List<TicketSeatId> seatIds) {
        final var selected = new LinkedHashSet<>(seatIds);
        return repository
                .findAllByTicketEventIdAndCustomerAccountId(
                        ticketEventId.value(), customerAccountId.value())
                .stream()
                .map(TicketPurchaseMapper::toDomain)
                .filter(purchase -> new LinkedHashSet<>(purchase.seatIds()).equals(selected))
                .findFirst();
    }
}

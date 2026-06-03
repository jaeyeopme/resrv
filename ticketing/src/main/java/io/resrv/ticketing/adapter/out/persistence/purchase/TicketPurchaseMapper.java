package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchase;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeatId;

final class TicketPurchaseMapper {

    private TicketPurchaseMapper() {}

    static TicketPurchaseJpaEntity toEntity(final TicketPurchase purchase) {
        final var entity =
                new TicketPurchaseJpaEntity(
                        purchase.id().value(),
                        purchase.ticketEventId().value(),
                        purchase.customerAccountId().value(),
                        purchase.confirmedAt());
        entity.replaceSeats(
                purchase.seatIds().stream()
                        .map(seatId -> new TicketPurchaseSeatJpaEntity(seatId.value()))
                        .toList());
        return entity;
    }

    static TicketPurchase toDomain(final TicketPurchaseJpaEntity entity) {
        return TicketPurchase.reconstitute(
                TicketPurchaseId.of(entity.id()),
                TicketEventId.of(entity.ticketEventId()),
                AccountId.of(entity.customerAccountId()),
                entity.seats().stream().map(seat -> TicketSeatId.of(seat.ticketSeatId())).toList(),
                entity.confirmedAt());
    }
}

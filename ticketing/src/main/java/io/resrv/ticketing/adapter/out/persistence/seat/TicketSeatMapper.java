package io.resrv.ticketing.adapter.out.persistence.seat;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.purchase.TicketPurchaseId;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;

final class TicketSeatMapper {

    private TicketSeatMapper() {}

    static TicketSeatJpaEntity toEntity(final TicketSeat seat) {
        return new TicketSeatJpaEntity(
                seat.id().value(),
                seat.ticketEventId().value(),
                seat.displayLabel(),
                seat.status(),
                seat.purchasedAt(),
                seat.purchaseId() == null ? null : seat.purchaseId().value());
    }

    static TicketSeat toDomain(final TicketSeatJpaEntity entity) {
        return TicketSeat.reconstitute(
                TicketSeatId.of(entity.id()),
                TicketEventId.of(entity.ticketEventId()),
                entity.displayLabel(),
                entity.status(),
                entity.purchasedAt(),
                entity.purchaseId() == null ? null : TicketPurchaseId.of(entity.purchaseId()));
    }
}

package io.resrv.ticketing.adapter.out.persistence.event;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.event.TicketEventProfile;
import io.resrv.ticketing.domain.event.TicketSaleWindow;

final class TicketEventMapper {

    private TicketEventMapper() {}

    static TicketEventJpaEntity toEntity(final TicketEvent event) {
        return new TicketEventJpaEntity(
                event.id().value(),
                event.businessId().value(),
                event.profile().title(),
                event.profile().eventStartAt(),
                event.profile().eventEndAt(),
                event.profile().timezone().value().getId(),
                event.saleWindow().startAt(),
                event.saleWindow().endAt(),
                event.saleWindow().timezone().value().getId(),
                event.status(),
                event.createdAt(),
                event.updatedAt());
    }

    static TicketEvent toDomain(final TicketEventJpaEntity entity) {
        return TicketEvent.reconstitute(
                TicketEventId.of(entity.id()),
                BusinessId.of(entity.businessId()),
                new TicketEventProfile(
                        entity.title(),
                        entity.eventStartAt(),
                        entity.eventEndAt(),
                        Timezone.of(entity.eventTimezone())),
                new TicketSaleWindow(
                        entity.saleStartAt(),
                        entity.saleEndAt(),
                        Timezone.of(entity.saleTimezone())),
                entity.status(),
                entity.createdAt(),
                entity.updatedAt());
    }
}

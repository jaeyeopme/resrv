package io.resrv.ticketing.application.event.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.event.TicketEventStatus;
import java.time.Instant;

public record TicketEventResult(
        TicketEventId id,
        BusinessId businessId,
        String title,
        Instant eventStartAt,
        Instant eventEndAt,
        Timezone eventTimezone,
        Instant saleStartAt,
        Instant saleEndAt,
        Timezone saleTimezone,
        TicketEventStatus status) {

    public static TicketEventResult from(final TicketEvent event) {
        return new TicketEventResult(
                event.id(),
                event.businessId(),
                event.profile().title(),
                event.profile().eventStartAt(),
                event.profile().eventEndAt(),
                event.profile().timezone(),
                event.saleWindow().startAt(),
                event.saleWindow().endAt(),
                event.saleWindow().timezone(),
                event.status());
    }
}

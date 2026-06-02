package io.resrv.ticketing.application.event.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Objects;

public record CreateTicketEventCommand(
        BusinessId businessId,
        String title,
        Instant eventStartAt,
        Instant eventEndAt,
        Timezone eventTimezone,
        Instant saleStartAt,
        Instant saleEndAt,
        Timezone saleTimezone) {

    public CreateTicketEventCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
    }
}

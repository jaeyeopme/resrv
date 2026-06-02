package io.resrv.ticketing.domain.event;

import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Objects;

public record TicketSaleWindow(Instant startAt, Instant endAt, Timezone timezone) {

    public TicketSaleWindow {
        Objects.requireNonNull(startAt, "Sale window start time must not be null");
        Objects.requireNonNull(endAt, "Sale window end time must not be null");
        Objects.requireNonNull(timezone, "Sale window timezone must not be null");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException(
                    "Sale window start time must be before sale window end time");
        }
    }
}

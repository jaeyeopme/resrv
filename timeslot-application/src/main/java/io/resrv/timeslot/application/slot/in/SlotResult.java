package io.resrv.timeslot.application.slot.in;

import io.resrv.timeslot.domain.slot.Slot;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SlotResult(
        String id,
        UUID businessId,
        UUID resourceId,
        Instant startAt,
        Instant endAt,
        OffsetDateTime startAtBusinessTime,
        OffsetDateTime endAtBusinessTime) {

    public static SlotResult from(final Slot slot) {
        return new SlotResult(
                slot.id().value(),
                slot.businessId().value(),
                slot.resourceId().value(),
                slot.startAt(),
                slot.endAt(),
                slot.startAtBusinessTime(),
                slot.endAtBusinessTime());
    }
}

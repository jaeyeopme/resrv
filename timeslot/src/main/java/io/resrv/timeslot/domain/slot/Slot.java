package io.resrv.timeslot.domain.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Slot(
        SlotId id,
        BusinessId businessId,
        ResourceId resourceId,
        Instant startAt,
        Instant endAt,
        OffsetDateTime startAtBusinessTime,
        OffsetDateTime endAtBusinessTime) {

    public Slot {
        Objects.requireNonNull(id, "Slot id must not be null");
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(startAt, "Slot start must not be null");
        Objects.requireNonNull(endAt, "Slot end must not be null");
        Objects.requireNonNull(startAtBusinessTime, "Business slot start must not be null");
        Objects.requireNonNull(endAtBusinessTime, "Business slot end must not be null");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Slot start must be before end");
        }
    }
}

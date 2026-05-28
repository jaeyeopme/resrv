package io.resrv.timeslot.application.discovery.in;

import java.time.OffsetDateTime;
import java.util.Objects;

public record PublicSlotDiscoveryResult(
        String slotId, OffsetDateTime startAt, OffsetDateTime endAt, boolean available) {

    public PublicSlotDiscoveryResult {
        Objects.requireNonNull(slotId, "Slot id must not be null");
        Objects.requireNonNull(startAt, "Slot start must not be null");
        Objects.requireNonNull(endAt, "Slot end must not be null");
    }
}

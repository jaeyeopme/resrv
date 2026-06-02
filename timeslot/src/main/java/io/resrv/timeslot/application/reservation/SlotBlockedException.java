package io.resrv.timeslot.application.reservation;

import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;

public final class SlotBlockedException extends RuntimeException {

    public SlotBlockedException(final ResourceId resourceId, final Instant startAt) {
        super("Slot is blocked for resource " + resourceId.value() + " at " + startAt);
    }
}

package io.resrv.timeslot.application.reservation;

import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;

public final class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(final ResourceId resourceId, final Instant startAt) {
        super("Slot is unavailable for resource " + resourceId.value() + " at " + startAt);
    }
}

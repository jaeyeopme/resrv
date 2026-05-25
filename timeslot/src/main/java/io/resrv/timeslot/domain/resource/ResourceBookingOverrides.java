package io.resrv.timeslot.domain.resource;

import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;

public record ResourceBookingOverrides(
        SlotDuration slotDuration, HoldTtl holdTtl, CancellationWindow cancellationWindow) {

    public static ResourceBookingOverrides none() {
        return new ResourceBookingOverrides(null, null, null);
    }
}

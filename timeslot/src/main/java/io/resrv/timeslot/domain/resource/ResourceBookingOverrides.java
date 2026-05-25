package io.resrv.timeslot.domain.resource;

import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.util.Objects;

public record ResourceBookingOverrides(
        SlotDuration slotDuration, HoldTtl holdTtl, CancellationWindow cancellationWindow) {

    public static ResourceBookingOverrides none() {
        return new ResourceBookingOverrides(null, null, null);
    }

    public EffectiveBookingPolicy resolve(final BusinessBookingSettings settings) {
        Objects.requireNonNull(settings, "Business booking settings must not be null");
        return new EffectiveBookingPolicy(
                slotDuration == null ? settings.slotDuration() : slotDuration,
                holdTtl == null ? settings.holdTtl() : holdTtl,
                cancellationWindow == null ? settings.cancellationWindow() : cancellationWindow,
                settings.maxAdvanceBookingDays());
    }
}

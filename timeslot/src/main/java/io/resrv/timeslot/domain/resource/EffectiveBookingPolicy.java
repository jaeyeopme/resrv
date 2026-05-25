package io.resrv.timeslot.domain.resource;

import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.util.Objects;

public record EffectiveBookingPolicy(
        SlotDuration slotDuration,
        HoldTtl holdTtl,
        CancellationWindow cancellationWindow,
        MaxAdvanceBookingDays maxAdvanceBookingDays) {

    public EffectiveBookingPolicy {
        Objects.requireNonNull(slotDuration, "Slot duration must not be null");
        Objects.requireNonNull(holdTtl, "Hold TTL must not be null");
        Objects.requireNonNull(cancellationWindow, "Cancellation window must not be null");
        Objects.requireNonNull(maxAdvanceBookingDays, "Max advance booking days must not be null");
    }
}

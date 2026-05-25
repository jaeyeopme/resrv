package io.resrv.timeslot.domain.settings;

import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Objects;

public record BusinessBookingSettings(
        BusinessId businessId,
        SlotDuration slotDuration,
        HoldTtl holdTtl,
        CancellationWindow cancellationWindow,
        MaxAdvanceBookingDays maxAdvanceBookingDays,
        Instant createdAt,
        Instant updatedAt) {

    public BusinessBookingSettings {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(slotDuration, "Slot duration must not be null");
        Objects.requireNonNull(holdTtl, "Hold TTL must not be null");
        Objects.requireNonNull(cancellationWindow, "Cancellation window must not be null");
        Objects.requireNonNull(maxAdvanceBookingDays, "Max advance booking days must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static BusinessBookingSettings create(
            final BusinessId businessId,
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final MaxAdvanceBookingDays maxAdvanceBookingDays,
            final Instant now) {
        return new BusinessBookingSettings(
                businessId,
                slotDuration,
                holdTtl,
                cancellationWindow,
                maxAdvanceBookingDays,
                now,
                now);
    }

    public static BusinessBookingSettings reconstitute(
            final BusinessId businessId,
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final MaxAdvanceBookingDays maxAdvanceBookingDays,
            final Instant createdAt,
            final Instant updatedAt) {
        return new BusinessBookingSettings(
                businessId,
                slotDuration,
                holdTtl,
                cancellationWindow,
                maxAdvanceBookingDays,
                createdAt,
                updatedAt);
    }

    public BusinessBookingSettings update(
            final SlotDuration slotDuration,
            final HoldTtl holdTtl,
            final CancellationWindow cancellationWindow,
            final MaxAdvanceBookingDays maxAdvanceBookingDays,
            final Instant now) {
        return new BusinessBookingSettings(
                businessId,
                slotDuration,
                holdTtl,
                cancellationWindow,
                maxAdvanceBookingDays,
                createdAt,
                now);
    }
}

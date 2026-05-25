package io.resrv.timeslot.application.settings.in;

import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import java.time.Instant;
import java.util.UUID;

public record BusinessBookingSettingsResult(
        UUID businessId,
        int slotDurationMinutes,
        int holdTtlMinutes,
        int cancellationWindowMinutes,
        int maxAdvanceBookingDays,
        Instant createdAt,
        Instant updatedAt) {

    public static BusinessBookingSettingsResult from(final BusinessBookingSettings settings) {
        return new BusinessBookingSettingsResult(
                settings.businessId().value(),
                settings.slotDuration().minutes(),
                settings.holdTtl().minutes(),
                settings.cancellationWindow().minutes(),
                settings.maxAdvanceBookingDays().days(),
                settings.createdAt(),
                settings.updatedAt());
    }
}

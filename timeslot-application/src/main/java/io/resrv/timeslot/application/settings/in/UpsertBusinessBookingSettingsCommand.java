package io.resrv.timeslot.application.settings.in;

import io.resrv.shared.kernel.BusinessId;
import java.util.Objects;

public record UpsertBusinessBookingSettingsCommand(
        BusinessId businessId,
        int slotDurationMinutes,
        int holdTtlMinutes,
        int cancellationWindowMinutes,
        int maxAdvanceBookingDays) {

    public UpsertBusinessBookingSettingsCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
    }
}

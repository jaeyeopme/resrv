package io.resrv.timeslot.application.schedule.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ReplaceDateScheduleOverrideCommand(
        BusinessId businessId,
        ResourceId resourceId,
        LocalDate date,
        List<ScheduleWindow> windows) {

    public ReplaceDateScheduleOverrideCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(date, "Date must not be null");
        Objects.requireNonNull(windows, "Schedule windows must not be null");
        windows = List.copyOf(windows);
    }
}

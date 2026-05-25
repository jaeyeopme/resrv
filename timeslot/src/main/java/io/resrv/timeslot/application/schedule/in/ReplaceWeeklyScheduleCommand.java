package io.resrv.timeslot.application.schedule.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

public record ReplaceWeeklyScheduleCommand(
        BusinessId businessId,
        ResourceId resourceId,
        DayOfWeek dayOfWeek,
        List<ScheduleWindow> windows) {

    public ReplaceWeeklyScheduleCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(dayOfWeek, "Day of week must not be null");
        Objects.requireNonNull(windows, "Schedule windows must not be null");
        windows = List.copyOf(windows);
    }
}

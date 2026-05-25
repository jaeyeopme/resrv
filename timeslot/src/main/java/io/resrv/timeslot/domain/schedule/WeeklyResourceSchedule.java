package io.resrv.timeslot.domain.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WeeklyResourceSchedule(
        BusinessId businessId,
        ResourceId resourceId,
        DayOfWeek dayOfWeek,
        List<ScheduleWindow> windows,
        Instant createdAt,
        Instant updatedAt) {

    public WeeklyResourceSchedule {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(dayOfWeek, "Day of week must not be null");
        ScheduleWindow.validateNoOverlap(windows);
        windows = List.copyOf(windows);
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static WeeklyResourceSchedule create(
            final BusinessId businessId,
            final ResourceId resourceId,
            final DayOfWeek dayOfWeek,
            final List<ScheduleWindow> windows,
            final Instant now) {
        return new WeeklyResourceSchedule(businessId, resourceId, dayOfWeek, windows, now, now);
    }

    public static WeeklyResourceSchedule reconstitute(
            final BusinessId businessId,
            final ResourceId resourceId,
            final DayOfWeek dayOfWeek,
            final List<ScheduleWindow> windows,
            final Instant createdAt,
            final Instant updatedAt) {
        return new WeeklyResourceSchedule(
                businessId, resourceId, dayOfWeek, windows, createdAt, updatedAt);
    }

    public WeeklyResourceSchedule replaceWindows(
            final List<ScheduleWindow> windows, final Instant now) {
        return new WeeklyResourceSchedule(
                businessId, resourceId, dayOfWeek, windows, createdAt, now);
    }
}

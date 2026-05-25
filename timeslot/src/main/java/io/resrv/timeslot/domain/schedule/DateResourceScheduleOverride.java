package io.resrv.timeslot.domain.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record DateResourceScheduleOverride(
        BusinessId businessId,
        ResourceId resourceId,
        LocalDate date,
        List<ScheduleWindow> windows,
        Instant createdAt,
        Instant updatedAt) {

    public DateResourceScheduleOverride {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(date, "Date must not be null");
        ScheduleWindow.validateNoOverlap(windows);
        windows = List.copyOf(windows);
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static DateResourceScheduleOverride create(
            final BusinessId businessId,
            final ResourceId resourceId,
            final LocalDate date,
            final List<ScheduleWindow> windows,
            final Instant now) {
        return new DateResourceScheduleOverride(businessId, resourceId, date, windows, now, now);
    }

    public static DateResourceScheduleOverride reconstitute(
            final BusinessId businessId,
            final ResourceId resourceId,
            final LocalDate date,
            final List<ScheduleWindow> windows,
            final Instant createdAt,
            final Instant updatedAt) {
        return new DateResourceScheduleOverride(
                businessId, resourceId, date, windows, createdAt, updatedAt);
    }

    public DateResourceScheduleOverride replaceWindows(
            final List<ScheduleWindow> windows, final Instant now) {
        return new DateResourceScheduleOverride(
                businessId, resourceId, date, windows, createdAt, now);
    }
}

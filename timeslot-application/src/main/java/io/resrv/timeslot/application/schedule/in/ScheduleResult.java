package io.resrv.timeslot.application.schedule.in;

import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleResult(
        UUID businessId,
        UUID resourceId,
        DayOfWeek dayOfWeek,
        LocalDate date,
        List<ScheduleWindow> windows,
        Instant createdAt,
        Instant updatedAt) {

    public ScheduleResult {
        windows = List.copyOf(windows);
    }

    public static ScheduleResult from(final WeeklyResourceSchedule schedule) {
        return new ScheduleResult(
                schedule.businessId().value(),
                schedule.resourceId().value(),
                schedule.dayOfWeek(),
                null,
                schedule.windows(),
                schedule.createdAt(),
                schedule.updatedAt());
    }

    public static ScheduleResult from(final DateResourceScheduleOverride override) {
        return new ScheduleResult(
                override.businessId().value(),
                override.resourceId().value(),
                null,
                override.date(),
                override.windows(),
                override.createdAt(),
                override.updatedAt());
    }
}

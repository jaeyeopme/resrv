package io.resrv.timeslot.application.schedule.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

public interface ResourceScheduleQueryPort {

    Optional<WeeklyResourceSchedule> findWeekly(
            BusinessId businessId, ResourceId resourceId, DayOfWeek dayOfWeek);

    Optional<DateResourceScheduleOverride> findDateOverride(
            BusinessId businessId, ResourceId resourceId, LocalDate date);
}

package io.resrv.timeslot.application.schedule.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import java.time.LocalDate;

public interface ResourceScheduleCommandPort {

    void saveWeekly(WeeklyResourceSchedule schedule);

    void saveDateOverride(DateResourceScheduleOverride override);

    void deleteDateOverride(BusinessId businessId, ResourceId resourceId, LocalDate date);
}

package io.resrv.timeslot.application.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.in.ReplaceDateScheduleOverrideCommand;
import io.resrv.timeslot.application.schedule.in.ReplaceWeeklyScheduleCommand;
import io.resrv.timeslot.application.schedule.in.ScheduleResult;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleCommandPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceScheduleService {

    private final ResourceQueryPort resourceQueryPort;
    private final ResourceScheduleCommandPort commandPort;
    private final ResourceScheduleQueryPort queryPort;
    private final Clock clock;

    public ResourceScheduleService(
            final ResourceQueryPort resourceQueryPort,
            final ResourceScheduleCommandPort commandPort,
            final ResourceScheduleQueryPort queryPort,
            final Clock clock) {
        this.resourceQueryPort = resourceQueryPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    public ScheduleResult replaceWeekly(final ReplaceWeeklyScheduleCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        ScheduleWindow.validateNoOverlap(command.windows());
        ensureResourceBelongsToBusiness(command.businessId(), command.resourceId());

        final var now = clock.instant();
        final var schedule =
                queryPort
                        .findWeekly(command.businessId(), command.resourceId(), command.dayOfWeek())
                        .map(existing -> existing.replaceWindows(command.windows(), now))
                        .orElseGet(
                                () ->
                                        WeeklyResourceSchedule.create(
                                                command.businessId(),
                                                command.resourceId(),
                                                command.dayOfWeek(),
                                                command.windows(),
                                                now));
        commandPort.saveWeekly(schedule);
        return ScheduleResult.from(schedule);
    }

    public ScheduleResult replaceDateOverride(final ReplaceDateScheduleOverrideCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        ScheduleWindow.validateNoOverlap(command.windows());
        ensureResourceBelongsToBusiness(command.businessId(), command.resourceId());

        final var now = clock.instant();
        final var override =
                queryPort
                        .findDateOverride(
                                command.businessId(), command.resourceId(), command.date())
                        .map(existing -> existing.replaceWindows(command.windows(), now))
                        .orElseGet(
                                () ->
                                        DateResourceScheduleOverride.create(
                                                command.businessId(),
                                                command.resourceId(),
                                                command.date(),
                                                command.windows(),
                                                now));
        commandPort.saveDateOverride(override);
        return ScheduleResult.from(override);
    }

    public void deleteDateOverride(
            final BusinessId businessId, final ResourceId resourceId, final LocalDate date) {
        Objects.requireNonNull(date, "Date must not be null");
        ensureResourceBelongsToBusiness(businessId, resourceId);

        commandPort.deleteDateOverride(businessId, resourceId, date);
    }

    private void ensureResourceBelongsToBusiness(
            final BusinessId businessId, final ResourceId resourceId) {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        if (resourceQueryPort.findByBusinessIdAndId(businessId, resourceId).isEmpty()) {
            throw new ResourceNotAvailableException(businessId, resourceId);
        }
    }
}

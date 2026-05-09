package io.resrv.application.availability;

import io.resrv.application.availability.in.DateAvailabilityOverrideResult;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.WeeklyAvailabilityResult;
import io.resrv.application.availability.out.DateAvailabilityOverrideCommandPort;
import io.resrv.application.availability.out.DateAvailabilityOverrideQueryPort;
import io.resrv.application.availability.out.WeeklyAvailabilityCommandPort;
import io.resrv.application.availability.out.WeeklyAvailabilityQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AvailabilityService
        implements UpsertWeeklyAvailabilityUseCase,
                DeleteWeeklyAvailabilityUseCase,
                UpsertDateAvailabilityOverrideUseCase,
                DeleteDateAvailabilityOverrideUseCase {

    private final Clock clock;
    private final ResourceQueryPort resourceQueryPort;
    private final WeeklyAvailabilityCommandPort weeklyCommandPort;
    private final WeeklyAvailabilityQueryPort weeklyQueryPort;
    private final DateAvailabilityOverrideCommandPort overrideCommandPort;
    private final DateAvailabilityOverrideQueryPort overrideQueryPort;

    AvailabilityService(
            final Clock clock,
            final ResourceQueryPort resourceQueryPort,
            final WeeklyAvailabilityCommandPort weeklyCommandPort,
            final WeeklyAvailabilityQueryPort weeklyQueryPort,
            final DateAvailabilityOverrideCommandPort overrideCommandPort,
            final DateAvailabilityOverrideQueryPort overrideQueryPort) {
        this.clock = clock;
        this.resourceQueryPort = resourceQueryPort;
        this.weeklyCommandPort = weeklyCommandPort;
        this.weeklyQueryPort = weeklyQueryPort;
        this.overrideCommandPort = overrideCommandPort;
        this.overrideQueryPort = overrideQueryPort;
    }

    @Override
    public WeeklyAvailabilityResult upsert(final UpsertWeeklyAvailabilityCommand command) {
        ensureActiveResource(command.tenantId(), command.resourceId());
        final var now = clock.instant();
        final var availability =
                weeklyQueryPort
                        .findByTenantIdAndResourceIdAndDayOfWeek(
                                command.tenantId(), command.resourceId(), command.dayOfWeek())
                        .map(
                                existing ->
                                        existing.update(
                                                command.startTime(), command.endTime(), now))
                        .orElseGet(
                                () ->
                                        WeeklyAvailability.create(
                                                command.tenantId(),
                                                command.resourceId(),
                                                command.dayOfWeek(),
                                                command.startTime(),
                                                command.endTime(),
                                                now));
        weeklyCommandPort.save(availability);
        return WeeklyAvailabilityResult.from(availability);
    }

    @Override
    public DateAvailabilityOverrideResult upsert(
            final UpsertDateAvailabilityOverrideCommand command) {
        ensureActiveResource(command.tenantId(), command.resourceId());
        final var now = clock.instant();
        final var override =
                overrideQueryPort
                        .findByTenantIdAndResourceIdAndDate(
                                command.tenantId(), command.resourceId(), command.date())
                        .map(
                                existing ->
                                        command.closed()
                                                ? existing.updateClosed(now)
                                                : existing.updateOpen(
                                                        command.startTime(),
                                                        command.endTime(),
                                                        now))
                        .orElseGet(
                                () ->
                                        command.closed()
                                                ? DateAvailabilityOverride.closed(
                                                        command.tenantId(),
                                                        command.resourceId(),
                                                        command.date(),
                                                        now)
                                                : DateAvailabilityOverride.open(
                                                        command.tenantId(),
                                                        command.resourceId(),
                                                        command.date(),
                                                        command.startTime(),
                                                        command.endTime(),
                                                        now));
        overrideCommandPort.save(override);
        return DateAvailabilityOverrideResult.from(override);
    }

    @Override
    public void delete(final DeleteWeeklyAvailabilityCommand command) {
        ensureActiveResource(command.tenantId(), command.resourceId());
        weeklyCommandPort.deleteByTenantIdAndResourceIdAndDayOfWeek(
                command.tenantId(), command.resourceId(), command.dayOfWeek());
    }

    @Override
    public void delete(final DeleteDateAvailabilityOverrideCommand command) {
        ensureActiveResource(command.tenantId(), command.resourceId());
        overrideCommandPort.deleteByTenantIdAndResourceIdAndDate(
                command.tenantId(), command.resourceId(), command.date());
    }

    private void ensureActiveResource(final TenantId tenantId, final ResourceId resourceId) {
        final var resource =
                resourceQueryPort
                        .findByTenantIdAndId(tenantId, resourceId)
                        .orElseThrow(() -> new ResourceNotFoundException(tenantId, resourceId));
        if (resource.status() != ResourceStatus.ACTIVE) {
            throw new ResourceNotFoundException(tenantId, resourceId);
        }
    }
}

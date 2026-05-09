package io.resrv.application.availability.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpsertWeeklyAvailabilityCommand(
        TenantId tenantId,
        ResourceId resourceId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {}

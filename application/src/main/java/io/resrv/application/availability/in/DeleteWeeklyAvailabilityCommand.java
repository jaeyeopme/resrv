package io.resrv.application.availability.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;

public record DeleteWeeklyAvailabilityCommand(
        TenantId tenantId, ResourceId resourceId, DayOfWeek dayOfWeek) {}

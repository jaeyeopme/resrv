package io.resrv.application.availability.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpsertDateAvailabilityOverrideCommand(
        TenantId tenantId,
        ResourceId resourceId,
        LocalDate date,
        boolean closed,
        LocalTime startTime,
        LocalTime endTime) {}

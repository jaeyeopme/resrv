package io.resrv.application.availability.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;

public record UpsertDateAvailabilityOverrideCommand(
        TenantId tenantId,
        ResourceId resourceId,
        LocalDate date,
        boolean closed,
        @Nullable LocalTime startTime,
        @Nullable LocalTime endTime) {}

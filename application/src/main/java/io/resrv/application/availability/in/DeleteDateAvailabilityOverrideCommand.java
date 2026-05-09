package io.resrv.application.availability.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;

public record DeleteDateAvailabilityOverrideCommand(
        TenantId tenantId, ResourceId resourceId, LocalDate date) {}

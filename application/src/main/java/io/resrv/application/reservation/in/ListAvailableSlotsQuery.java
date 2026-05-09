package io.resrv.application.reservation.in;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;

public record ListAvailableSlotsQuery(TenantId tenantId, ResourceId resourceId, LocalDate date) {}

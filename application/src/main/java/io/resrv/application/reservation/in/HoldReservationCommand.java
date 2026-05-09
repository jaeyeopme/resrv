package io.resrv.application.reservation.in;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;

public record HoldReservationCommand(
        TenantId tenantId, CustomerId customerId, ResourceId resourceId, Instant startAt) {}

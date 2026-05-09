package io.resrv.application.reservation.in;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.tenant.TenantId;

public record ListCustomerReservationsQuery(TenantId tenantId, CustomerId customerId) {}

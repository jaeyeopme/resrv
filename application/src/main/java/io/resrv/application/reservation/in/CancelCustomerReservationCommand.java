package io.resrv.application.reservation.in;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.tenant.TenantId;

public record CancelCustomerReservationCommand(
        TenantId tenantId, CustomerId customerId, ReservationId reservationId) {}

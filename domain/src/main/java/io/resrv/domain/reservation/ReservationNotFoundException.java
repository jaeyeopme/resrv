package io.resrv.domain.reservation;

import io.resrv.domain.tenant.TenantId;

public final class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(
            final TenantId tenantId, final ReservationId reservationId) {
        super(
                "Reservation '%s' was not found for tenant '%s'"
                        .formatted(reservationId.value(), tenantId.value()));
    }
}

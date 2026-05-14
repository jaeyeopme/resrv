package io.resrv.application.reservation.in;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record ListAdminReservationsQuery(
        TenantId tenantId,
        LocalDate date,
        Optional<ResourceId> resourceId,
        Optional<CustomerId> customerId,
        Optional<ReservationStatus> status) {

    public ListAdminReservationsQuery {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(date, "Reservation query date must not be null");
        resourceId = Objects.requireNonNullElse(resourceId, Optional.empty());
        customerId = Objects.requireNonNullElse(customerId, Optional.empty());
        status = Objects.requireNonNullElse(status, Optional.empty());
    }
}

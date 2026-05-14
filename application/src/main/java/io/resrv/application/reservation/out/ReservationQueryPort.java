package io.resrv.application.reservation.out;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationQueryPort {

    Optional<Reservation> findByTenantIdAndId(TenantId tenantId, ReservationId reservationId);

    List<Reservation> findByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    List<Reservation> findByTenantIdAndResourceIdBetween(
            TenantId tenantId, ResourceId resourceId, Instant rangeStart, Instant rangeEnd);

    List<Reservation> findByTenantIdBetweenWithFilters(
            TenantId tenantId,
            Instant rangeStart,
            Instant rangeEnd,
            Optional<ResourceId> resourceId,
            Optional<CustomerId> customerId,
            Optional<ReservationStatus> status);

    boolean existsActiveOverlap(
            TenantId tenantId, ResourceId resourceId, Instant startAt, Instant endAt);
}

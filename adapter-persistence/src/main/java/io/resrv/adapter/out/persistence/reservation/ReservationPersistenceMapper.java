package io.resrv.adapter.out.persistence.reservation;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {}

    static ReservationJpaEntity toJpaEntity(final Reservation reservation) {
        return new ReservationJpaEntity(
                reservation.id().value(),
                reservation.tenantId().value(),
                reservation.resourceId().value(),
                reservation.customerId().value(),
                reservation.startAt(),
                reservation.endAt(),
                reservation.status().name(),
                reservation.holdExpiresAt(),
                reservation.createdAt(),
                reservation.updatedAt(),
                reservation.confirmedAt(),
                reservation.cancelledAt());
    }

    static Reservation toDomain(final ReservationJpaEntity entity) {
        return Reservation.reconstitute(
                ReservationId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                ResourceId.of(entity.getResourceId()),
                CustomerId.of(entity.getCustomerId()),
                entity.getStartAt(),
                entity.getEndAt(),
                ReservationStatus.valueOf(entity.getStatus()),
                entity.getHoldExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getConfirmedAt(),
                entity.getCancelledAt());
    }
}

package io.resrv.adapter.out.persistence.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface ReservationJpaRepository extends CrudRepository<ReservationJpaEntity, UUID> {

    Optional<ReservationJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<ReservationJpaEntity> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.tenantId = :tenantId
              AND reservation.resourceId = :resourceId
              AND reservation.startAt < :rangeEnd
              AND reservation.endAt > :rangeStart
            """)
    List<ReservationJpaEntity> findByTenantIdAndResourceIdBetween(
            UUID tenantId, UUID resourceId, Instant rangeStart, Instant rangeEnd);

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.tenantId = :tenantId
              AND reservation.startAt < :rangeEnd
              AND reservation.endAt > :rangeStart
              AND (:resourceId IS NULL OR reservation.resourceId = :resourceId)
              AND (:customerId IS NULL OR reservation.customerId = :customerId)
              AND (:status IS NULL OR reservation.status = :status)
            ORDER BY reservation.startAt ASC, reservation.id ASC
            """)
    List<ReservationJpaEntity> findByTenantIdBetweenWithFilters(
            UUID tenantId,
            Instant rangeStart,
            Instant rangeEnd,
            UUID resourceId,
            UUID customerId,
            String status);

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.tenantId = :tenantId
              AND reservation.resourceId = :resourceId
              AND reservation.status IN ('HELD', 'CONFIRMED', 'CHECKED_IN')
              AND reservation.startAt < :endAt
              AND reservation.endAt > :startAt
            """)
    List<ReservationJpaEntity> findActiveOverlaps(
            UUID tenantId, UUID resourceId, Instant startAt, Instant endAt);

    List<ReservationJpaEntity> findByStatusAndHoldExpiresAtLessThanEqual(
            String status, Instant holdExpiresAt);
}

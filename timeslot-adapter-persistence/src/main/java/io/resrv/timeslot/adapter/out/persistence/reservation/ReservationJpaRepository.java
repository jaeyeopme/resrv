package io.resrv.timeslot.adapter.out.persistence.reservation;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.businessId = :businessId
              AND reservation.resourceId = :resourceId
              AND reservation.startAt < :endAt
              AND reservation.endAt > :startAt
              AND reservation.releasedAt IS NULL
              AND reservation.cancelledAt IS NULL
              AND reservation.noShowAt IS NULL
              AND (
                reservation.confirmedAt IS NOT NULL
                OR reservation.holdExpiresAt > :now
              )
            """)
    List<ReservationJpaEntity> findActiveBlockers(
            UUID businessId, UUID resourceId, Instant startAt, Instant endAt, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.businessId = :businessId
              AND reservation.id = :reservationId
            """)
    Optional<ReservationJpaEntity> findByBusinessIdAndIdForUpdate(
            UUID businessId, UUID reservationId);
}

package io.resrv.timeslot.adapter.out.persistence.reservation;

import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.businessId = :businessId
              AND reservation.startAt >= :startInclusive
              AND reservation.startAt < :endExclusive
              AND (:resourceId IS NULL OR reservation.resourceId = :resourceId)
              AND (:customerAccountId IS NULL
                OR reservation.customerAccountId = :customerAccountId)
            ORDER BY reservation.startAt ASC, reservation.createdAt ASC
            """)
    List<ReservationJpaEntity> findByBusinessDateWindow(
            UUID businessId,
            Instant startInclusive,
            Instant endExclusive,
            UUID resourceId,
            UUID customerAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.businessId = :businessId
              AND reservation.id = :reservationId
            """)
    Optional<ReservationJpaEntity> findByBusinessIdAndIdForUpdate(
            UUID businessId, UUID reservationId);

    @Query(
            """
            SELECT reservation FROM ReservationJpaEntity reservation
            WHERE reservation.customerAccountId = :accountId
              AND (
                :state IS NULL
                OR (:state = 'RELEASED'
                    AND reservation.releasedAt IS NOT NULL)
                OR (:state = 'CHECKED_IN'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NOT NULL)
                OR (:state = 'NO_SHOW'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NOT NULL)
                OR (:state = 'CUSTOMER_CANCELLED'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NULL
                    AND reservation.cancelledAt IS NOT NULL
                    AND reservation.cancelledBy = :customerCancelled)
                OR (:state = 'BUSINESS_CANCELLED'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NULL
                    AND reservation.cancelledAt IS NOT NULL
                    AND reservation.cancelledBy = :businessCancelled)
                OR (:state = 'CONFIRMED'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NULL
                    AND reservation.cancelledAt IS NULL
                    AND reservation.confirmedAt IS NOT NULL)
                OR (:state = 'EXPIRED'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NULL
                    AND reservation.cancelledAt IS NULL
                    AND reservation.confirmedAt IS NULL
                    AND reservation.holdExpiresAt <= :now)
                OR (:state = 'HELD'
                    AND reservation.releasedAt IS NULL
                    AND reservation.checkedInAt IS NULL
                    AND reservation.noShowAt IS NULL
                    AND reservation.cancelledAt IS NULL
                    AND reservation.confirmedAt IS NULL
                    AND reservation.holdExpiresAt > :now)
              )
              AND (
                :upcoming IS NULL
                OR :upcoming = false
                OR (
                  reservation.endAt > :now
                  AND reservation.releasedAt IS NULL
                  AND reservation.noShowAt IS NULL
                  AND reservation.cancelledAt IS NULL
                  AND (
                    reservation.checkedInAt IS NOT NULL
                    OR reservation.confirmedAt IS NOT NULL
                    OR (
                      reservation.confirmedAt IS NULL
                      AND reservation.checkedInAt IS NULL
                      AND reservation.holdExpiresAt > :now
                    )
                  )
                )
              )
            """)
    Page<ReservationJpaEntity> findByCustomerAccountId(
            UUID accountId,
            String state,
            Boolean upcoming,
            Instant now,
            ReservationCancellationActor customerCancelled,
            ReservationCancellationActor businessCancelled,
            Pageable pageable);
}

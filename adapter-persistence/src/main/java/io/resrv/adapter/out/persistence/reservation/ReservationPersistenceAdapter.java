package io.resrv.adapter.out.persistence.reservation;

import io.resrv.application.reservation.out.ReservationCommandPort;
import io.resrv.application.reservation.out.ReservationQueryPort;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.reservation.SlotUnavailableException;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class ReservationPersistenceAdapter implements ReservationCommandPort, ReservationQueryPort {

    private final ReservationJpaRepository reservationJpaRepository;
    private final EntityManager entityManager;

    ReservationPersistenceAdapter(
            final ReservationJpaRepository reservationJpaRepository,
            final EntityManager entityManager) {
        this.reservationJpaRepository = reservationJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Reservation reservation) {
        try {
            reservationJpaRepository.save(ReservationPersistenceMapper.toJpaEntity(reservation));
            entityManager.flush();
        } catch (final DataIntegrityViolationException | PersistenceException _) {
            throw new SlotUnavailableException(reservation.resourceId(), reservation.startAt());
        }
    }

    @Override
    public int expireHoldsDueAtOrBefore(final Instant now) {
        final var expiredReservations =
                reservationJpaRepository
                        .findByStatusAndHoldExpiresAtLessThanEqual(
                                ReservationStatus.HELD.name(), now)
                        .stream()
                        .map(ReservationPersistenceMapper::toDomain)
                        .map(reservation -> reservation.expire(now))
                        .map(ReservationPersistenceMapper::toJpaEntity)
                        .toList();
        reservationJpaRepository.saveAll(expiredReservations);
        entityManager.flush();
        return expiredReservations.size();
    }

    @Override
    public Optional<Reservation> findByTenantIdAndId(
            final TenantId tenantId, final ReservationId reservationId) {
        return reservationJpaRepository
                .findByTenantIdAndId(tenantId.value(), reservationId.value())
                .map(ReservationPersistenceMapper::toDomain);
    }

    @Override
    public List<Reservation> findByTenantIdAndCustomerId(
            final TenantId tenantId, final CustomerId customerId) {
        return reservationJpaRepository
                .findByTenantIdAndCustomerId(tenantId.value(), customerId.value())
                .stream()
                .map(ReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> findByTenantIdAndResourceIdBetween(
            final TenantId tenantId,
            final ResourceId resourceId,
            final Instant rangeStart,
            final Instant rangeEnd) {
        return reservationJpaRepository
                .findByTenantIdAndResourceIdBetween(
                        tenantId.value(), resourceId.value(), rangeStart, rangeEnd)
                .stream()
                .map(ReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveOverlap(
            final TenantId tenantId,
            final ResourceId resourceId,
            final Instant startAt,
            final Instant endAt) {
        return !reservationJpaRepository
                .findActiveOverlaps(tenantId.value(), resourceId.value(), startAt, endAt)
                .isEmpty();
    }
}

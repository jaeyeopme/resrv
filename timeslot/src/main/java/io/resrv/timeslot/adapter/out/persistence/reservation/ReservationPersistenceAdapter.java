package io.resrv.timeslot.adapter.out.persistence.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ReservationPersistenceAdapter implements ReservationCommandPort, ReservationQueryPort {

    private final ReservationJpaRepository repository;

    ReservationPersistenceAdapter(final ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final Reservation reservation) {
        repository.save(ReservationJpaEntity.fromDomain(reservation));
    }

    @Override
    public List<Reservation> findActiveBlockers(
            final BusinessId businessId,
            final ResourceId resourceId,
            final Instant startAt,
            final Instant endAt,
            final Instant now) {
        return repository
                .findActiveBlockers(businessId.value(), resourceId.value(), startAt, endAt, now)
                .stream()
                .map(ReservationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> findByBusinessDateWindow(
            final BusinessId businessId,
            final Instant startInclusive,
            final Instant endExclusive,
            final ResourceId resourceId,
            final AccountId customerAccountId) {
        return repository
                .findByBusinessDateWindow(
                        businessId.value(),
                        startInclusive,
                        endExclusive,
                        resourceId == null ? null : resourceId.value(),
                        customerAccountId == null ? null : customerAccountId.value())
                .stream()
                .map(ReservationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Reservation> findByBusinessIdAndIdForUpdate(
            final BusinessId businessId, final ReservationId reservationId) {
        return repository
                .findByBusinessIdAndIdForUpdate(businessId.value(), reservationId.value())
                .map(ReservationJpaEntity::toDomain);
    }
}

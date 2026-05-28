package io.resrv.timeslot.adapter.out.persistence.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationPage;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    public ReservationPage findByCustomerAccountId(
            final AccountId accountId,
            final int page,
            final int size,
            final ReservationState state,
            final Boolean upcoming,
            final Instant now) {
        final var result =
                repository.findByCustomerAccountId(
                        accountId.value(),
                        state == null ? null : state.name(),
                        upcoming,
                        now,
                        ReservationCancellationActor.CUSTOMER,
                        ReservationCancellationActor.BUSINESS,
                        customerHistoryPage(page, size));
        return new ReservationPage(
                result.getContent().stream().map(ReservationJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public Optional<Reservation> findById(final ReservationId reservationId) {
        return repository.findById(reservationId.value()).map(ReservationJpaEntity::toDomain);
    }

    private static PageRequest customerHistoryPage(final int page, final int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("startAt"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")));
    }
}

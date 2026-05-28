package io.resrv.timeslot.application.reservation.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationQueryPort {

    List<Reservation> findActiveBlockers(
            BusinessId businessId,
            ResourceId resourceId,
            Instant startAt,
            Instant endAt,
            Instant now);

    List<Reservation> findByBusinessDateWindow(
            BusinessId businessId,
            Instant startInclusive,
            Instant endExclusive,
            ResourceId resourceId,
            AccountId customerAccountId);

    ReservationPage findByCustomerAccountId(
            AccountId accountId,
            int page,
            int size,
            ReservationState state,
            Boolean upcoming,
            Instant now);

    Optional<Reservation> findById(ReservationId reservationId);
}

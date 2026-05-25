package io.resrv.timeslot.application.reservation.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.reservation.Reservation;
import java.time.Instant;
import java.util.List;

public interface ReservationQueryPort {

    List<Reservation> findActiveBlockers(
            BusinessId businessId,
            ResourceId resourceId,
            Instant startAt,
            Instant endAt,
            Instant now);
}

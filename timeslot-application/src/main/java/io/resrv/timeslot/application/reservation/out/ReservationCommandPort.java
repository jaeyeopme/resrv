package io.resrv.timeslot.application.reservation.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.timeslot.domain.reservation.Reservation;
import java.util.Optional;

public interface ReservationCommandPort {

    void save(Reservation reservation);

    Optional<Reservation> findByBusinessIdAndIdForUpdate(
            BusinessId businessId, ReservationId reservationId);
}

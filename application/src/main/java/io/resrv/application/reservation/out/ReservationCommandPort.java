package io.resrv.application.reservation.out;

import io.resrv.domain.reservation.Reservation;
import java.time.Instant;

public interface ReservationCommandPort {

    void save(Reservation reservation);

    int expireHoldsDueAtOrBefore(Instant now);
}

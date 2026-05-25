package io.resrv.timeslot.application.reservation.out;

import io.resrv.timeslot.domain.reservation.Reservation;

public interface ReservationCommandPort {

    void save(Reservation reservation);
}

package io.resrv.timeslot.application.discovery.in;

import io.resrv.timeslot.application.reservation.in.ReservationResult;

public interface HoldReservationByBusinessSlugUseCase {

    ReservationResult holdReservation(HoldReservationByBusinessSlugCommand command);
}

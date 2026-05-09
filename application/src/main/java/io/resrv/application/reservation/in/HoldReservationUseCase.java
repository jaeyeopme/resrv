package io.resrv.application.reservation.in;

public interface HoldReservationUseCase {

    ReservationResult hold(HoldReservationCommand command);
}

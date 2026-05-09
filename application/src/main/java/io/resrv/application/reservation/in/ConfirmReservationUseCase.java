package io.resrv.application.reservation.in;

public interface ConfirmReservationUseCase {

    ReservationResult confirm(ConfirmReservationCommand command);
}

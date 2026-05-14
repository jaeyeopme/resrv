package io.resrv.application.reservation.in;

public interface AdminCancelReservationUseCase {

    ReservationResult adminCancel(AdminCancelReservationCommand command);
}

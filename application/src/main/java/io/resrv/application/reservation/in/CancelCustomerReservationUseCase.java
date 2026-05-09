package io.resrv.application.reservation.in;

public interface CancelCustomerReservationUseCase {

    ReservationResult cancel(CancelCustomerReservationCommand command);
}

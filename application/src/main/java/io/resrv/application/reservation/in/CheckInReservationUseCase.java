package io.resrv.application.reservation.in;

public interface CheckInReservationUseCase {

    ReservationResult checkIn(CheckInReservationCommand command);
}

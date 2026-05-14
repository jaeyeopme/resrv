package io.resrv.application.reservation.in;

public interface MarkNoShowReservationUseCase {

    ReservationResult markNoShow(MarkNoShowReservationCommand command);
}

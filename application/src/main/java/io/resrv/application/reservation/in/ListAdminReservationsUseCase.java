package io.resrv.application.reservation.in;

import java.util.List;

public interface ListAdminReservationsUseCase {

    List<ReservationResult> listAdminReservations(ListAdminReservationsQuery query);
}

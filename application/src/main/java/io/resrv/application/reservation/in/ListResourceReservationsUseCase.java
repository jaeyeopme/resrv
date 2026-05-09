package io.resrv.application.reservation.in;

import java.util.List;

public interface ListResourceReservationsUseCase {

    List<ReservationResult> listResourceReservations(ListResourceReservationsQuery query);
}

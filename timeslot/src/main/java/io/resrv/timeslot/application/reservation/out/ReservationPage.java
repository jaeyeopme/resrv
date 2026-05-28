package io.resrv.timeslot.application.reservation.out;

import io.resrv.timeslot.domain.reservation.Reservation;
import java.util.List;
import java.util.Objects;

public record ReservationPage(
        List<Reservation> items, int page, int size, long totalElements, int totalPages) {

    public ReservationPage {
        items = List.copyOf(Objects.requireNonNull(items, "Items must not be null"));
    }
}

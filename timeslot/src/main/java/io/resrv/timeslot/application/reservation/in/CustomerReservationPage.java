package io.resrv.timeslot.application.reservation.in;

import java.util.List;
import java.util.Objects;

public record CustomerReservationPage(
        List<CustomerReservationResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public CustomerReservationPage {
        items = List.copyOf(Objects.requireNonNull(items, "Items must not be null"));
    }
}

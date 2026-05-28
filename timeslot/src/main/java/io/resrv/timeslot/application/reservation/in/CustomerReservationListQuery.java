package io.resrv.timeslot.application.reservation.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.util.Objects;

public record CustomerReservationListQuery(
        AccountId accountId, int page, int size, ReservationState state, Boolean upcoming) {

    public static final String DEFAULT_PAGE_VALUE = "0";
    public static final String DEFAULT_SIZE_VALUE = "20";
    public static final int MAX_SIZE = 100;

    public CustomerReservationListQuery {
        Objects.requireNonNull(accountId, "Account id must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}

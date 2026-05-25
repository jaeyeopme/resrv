package io.resrv.timeslot.domain.settings;

public record MaxAdvanceBookingDays(int days) {

    public MaxAdvanceBookingDays {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("Max advance booking days must be 1-365");
        }
    }
}

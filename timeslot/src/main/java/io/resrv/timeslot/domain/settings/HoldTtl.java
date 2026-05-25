package io.resrv.timeslot.domain.settings;

public record HoldTtl(int minutes) {

    public HoldTtl {
        if (minutes < 1 || minutes > 30) {
            throw new IllegalArgumentException("Hold TTL must be 1-30 minutes");
        }
    }
}

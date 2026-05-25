package io.resrv.timeslot.domain.settings;

public record CancellationWindow(int minutes) {

    public CancellationWindow {
        if (minutes < 0 || minutes > 10080) {
            throw new IllegalArgumentException("Cancellation window must be 0-10080 minutes");
        }
    }
}

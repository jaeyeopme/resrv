package io.resrv.timeslot.domain.settings;

public record SlotDuration(int minutes) {

    public SlotDuration {
        if (minutes < 5 || minutes > 480 || minutes % 5 != 0) {
            throw new IllegalArgumentException(
                    "Slot duration must be 5-480 minutes in 5 minute increments");
        }
    }
}

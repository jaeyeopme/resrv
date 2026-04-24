package io.resrv.domain.tenant;

public record SlotDuration(int minutes) {

    private static final int MIN_MINUTES = 30;
    private static final int MAX_MINUTES = 480;
    private static final int INCREMENT = 30;

    public SlotDuration {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new IllegalArgumentException(
                    "Slot duration must be %d-%d minutes, got %d"
                            .formatted(MIN_MINUTES, MAX_MINUTES, minutes));
        }
        if (minutes % INCREMENT != 0) {
            throw new IllegalArgumentException(
                    "Slot duration must be in %d-minute increments, got %d"
                            .formatted(INCREMENT, minutes));
        }
    }
}

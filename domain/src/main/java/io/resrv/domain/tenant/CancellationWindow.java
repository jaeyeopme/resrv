package io.resrv.domain.tenant;

import java.util.Objects;

public record CancellationWindow(int minutes) {

    public static final int DEFAULT_MINUTES = 0;

    public CancellationWindow {
        if (minutes < 0) {
            throw new IllegalArgumentException(
                    "Cancellation window must be 0 or more minutes, got %d".formatted(minutes));
        }
    }

    public static CancellationWindow of(final Integer minutes) {
        return new CancellationWindow(Objects.requireNonNullElse(minutes, DEFAULT_MINUTES));
    }
}

package io.resrv.domain.tenant;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record HoldTtl(int minutes) {

    public static final int DEFAULT_MINUTES = 15;
    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 30;

    public HoldTtl {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new IllegalArgumentException(
                    "Hold TTL must be %d-%d minutes, got %d"
                            .formatted(MIN_MINUTES, MAX_MINUTES, minutes));
        }
    }

    public static HoldTtl of(final @Nullable Integer minutes) {
        return new HoldTtl(Objects.requireNonNullElse(minutes, DEFAULT_MINUTES));
    }
}

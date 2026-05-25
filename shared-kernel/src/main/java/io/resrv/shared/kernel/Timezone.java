package io.resrv.shared.kernel;

import java.time.ZoneId;
import java.util.Objects;

public record Timezone(ZoneId value) {

    public Timezone {
        Objects.requireNonNull(value, "Timezone must not be null");
    }

    public static Timezone of(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Timezone must not be blank");
        }
        return new Timezone(ZoneId.of(value));
    }
}

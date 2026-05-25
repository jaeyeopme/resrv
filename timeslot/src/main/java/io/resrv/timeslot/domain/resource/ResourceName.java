package io.resrv.timeslot.domain.resource;

import java.util.Objects;

public record ResourceName(String value) {

    private static final int MAX_LENGTH = 100;

    public ResourceName {
        value = Objects.requireNonNull(value, "Resource name must not be null").strip();
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Resource name must be 1-100 characters");
        }
    }
}

package io.resrv.domain.resource;

import java.util.Objects;

public record ResourceName(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public ResourceName {
        value = Objects.requireNonNull(value, "ResourceName must not be null").strip();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Resource name must be %d-%d characters after trimming, got %d"
                            .formatted(MIN_LENGTH, MAX_LENGTH, value.length()));
        }
    }
}

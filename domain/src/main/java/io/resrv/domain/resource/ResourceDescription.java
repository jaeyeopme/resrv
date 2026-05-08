package io.resrv.domain.resource;

import org.jspecify.annotations.Nullable;

public record ResourceDescription(@Nullable String value) {

    private static final int MAX_LENGTH = 500;

    public ResourceDescription {
        if (value != null) {
            value = value.strip();
            if (value.isEmpty()) {
                value = null;
            } else if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Resource description must be at most %d characters after trimming, got %d"
                                .formatted(MAX_LENGTH, value.length()));
            }
        }
    }

    public static ResourceDescription empty() {
        return new ResourceDescription(null);
    }
}

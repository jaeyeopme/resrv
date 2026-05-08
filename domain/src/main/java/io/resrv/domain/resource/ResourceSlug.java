package io.resrv.domain.resource;

import java.util.Objects;
import java.util.regex.Pattern;

public record ResourceSlug(String value) {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]++(?>-[a-z0-9]++)*+$");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;

    public ResourceSlug {
        value = Objects.requireNonNull(value, "ResourceSlug must not be null");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Resource slug must be %d-%d characters, got %d"
                            .formatted(MIN_LENGTH, MAX_LENGTH, value.length()));
        }
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Resource slug must contain only lowercase letters, digits, and hyphens,"
                            + " no consecutive hyphens, and must start/end with a letter or digit");
        }
    }
}

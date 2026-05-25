package io.resrv.timeslot.domain.resource;

import java.util.Objects;
import java.util.regex.Pattern;

public record ResourceSlug(String value) {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;

    public ResourceSlug {
        value = Objects.requireNonNull(value, "Resource slug must not be null");
        if (value.length() < MIN_LENGTH
                || value.length() > MAX_LENGTH
                || !SLUG.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Resource slug must be 3-63 lowercase URL characters");
        }
    }
}

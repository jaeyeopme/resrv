package io.resrv.domain.tenant;

import java.util.regex.Pattern;

public record Slug(String value) {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]++(?>-[a-z0-9]++)*+$");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;

    public Slug {
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Slug must be %d-%d characters, got %d"
                            .formatted(MIN_LENGTH, MAX_LENGTH, value.length()));
        }
        if (!SLUG_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Slug must contain only lowercase letters, digits, and hyphens,"
                            + " no consecutive hyphens, and must start/end with a letter or digit");
        }
    }
}

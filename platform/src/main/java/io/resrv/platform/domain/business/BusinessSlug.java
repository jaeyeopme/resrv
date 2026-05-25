package io.resrv.platform.domain.business;

import java.util.regex.Pattern;

public record BusinessSlug(String value) {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    public BusinessSlug {
        if (value == null || !SLUG.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Business slug must be 3-63 lowercase URL characters");
        }
    }
}

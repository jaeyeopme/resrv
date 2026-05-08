package io.resrv.domain.customer;

import java.util.Locale;
import java.util.regex.Pattern;

public record CustomerEmail(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public CustomerEmail {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer email must not be blank");
        }
        final var normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid customer email format: " + value);
        }
        value = normalized;
    }
}

package io.resrv.platform.domain.account;

import java.util.Locale;
import java.util.regex.Pattern;

public record AccountEmail(String value) {

    private static final Pattern VALID_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public AccountEmail {
        if (value == null) {
            throw new IllegalArgumentException("Account email must be valid");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || !VALID_EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Account email must be valid");
        }
    }
}

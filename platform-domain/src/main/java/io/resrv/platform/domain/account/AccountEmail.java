package io.resrv.platform.domain.account;

import java.util.Locale;
import java.util.regex.Pattern;

public record AccountEmail(String value) {

    private static final Pattern EMAIL =
            Pattern.compile(
                    "^[A-Z0-9](?:[A-Z0-9._%+-]{0,62}[A-Z0-9])?@"
                            + "[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                            + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
                    Pattern.CASE_INSENSITIVE);

    public AccountEmail {
        if (value == null) {
            throw new IllegalArgumentException("Account email must be valid");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || !EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Account email must be valid");
        }
        final var localPart = value.substring(0, value.indexOf('@'));
        if (localPart.contains("..")) {
            throw new IllegalArgumentException("Account email must be valid");
        }
    }
}

package io.resrv.platform.domain.account;

public record AccountName(String value) {

    private static final int MAX_LENGTH = 100;

    public AccountName {
        if (value == null) {
            throw new IllegalArgumentException("Account name must be 1-100 characters");
        }
        value = value.trim();
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Account name must be 1-100 characters");
        }
    }
}

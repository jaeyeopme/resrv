package io.resrv.platform.domain.business;

public record BusinessName(String value) {

    private static final int MAX_LENGTH = 100;

    public BusinessName {
        if (value == null) {
            throw new IllegalArgumentException("Business name must be 1-100 characters");
        }
        value = value.trim();
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Business name must be 1-100 characters");
        }
    }
}

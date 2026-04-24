package io.resrv.domain.tenant;

public record TenantName(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public TenantName {
        value = value.strip();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Name must be %d-%d characters after trimming, got %d"
                            .formatted(MIN_LENGTH, MAX_LENGTH, value.length()));
        }
    }
}

package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "Account id must not be null");
    }

    public static AccountId create() {
        return of(UUID.randomUUID());
    }

    public static AccountId of(final UUID value) {
        return new AccountId(value);
    }
}

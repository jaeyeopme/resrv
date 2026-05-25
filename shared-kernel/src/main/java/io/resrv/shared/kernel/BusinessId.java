package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record BusinessId(UUID value) {

    public BusinessId {
        Objects.requireNonNull(value, "Business id must not be null");
    }

    public static BusinessId create() {
        return of(UUID.randomUUID());
    }

    public static BusinessId of(final UUID value) {
        return new BusinessId(value);
    }
}

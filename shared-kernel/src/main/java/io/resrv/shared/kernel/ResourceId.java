package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, "Resource id must not be null");
    }

    public static ResourceId create() {
        return of(UUID.randomUUID());
    }

    public static ResourceId of(final UUID value) {
        return new ResourceId(value);
    }
}

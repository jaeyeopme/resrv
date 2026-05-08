package io.resrv.domain.resource;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, "ResourceId must not be null");
    }

    public static ResourceId create() {
        return new ResourceId(Generators.timeBasedEpochGenerator().generate());
    }

    public static ResourceId of(final UUID value) {
        return new ResourceId(value);
    }
}

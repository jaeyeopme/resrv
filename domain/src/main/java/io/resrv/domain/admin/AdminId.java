package io.resrv.domain.admin;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record AdminId(UUID value) {

    public AdminId {
        Objects.requireNonNull(value, "AdminId must not be null");
    }

    public static AdminId create() {
        return new AdminId(Generators.timeBasedEpochGenerator().generate());
    }

    public static AdminId of(final UUID value) {
        return new AdminId(value);
    }
}

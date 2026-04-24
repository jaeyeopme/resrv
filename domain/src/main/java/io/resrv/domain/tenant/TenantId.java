package io.resrv.domain.tenant;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "TenantId must not be null");
    }

    public static TenantId create() {
        return new TenantId(Generators.timeBasedEpochGenerator().generate());
    }

    public static TenantId of(final UUID value) {
        return new TenantId(value);
    }
}

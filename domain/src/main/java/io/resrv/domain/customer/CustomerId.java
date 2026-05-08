package io.resrv.domain.customer;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId must not be null");
    }

    public static CustomerId create() {
        return new CustomerId(Generators.timeBasedEpochGenerator().generate());
    }

    public static CustomerId of(final UUID value) {
        return new CustomerId(value);
    }
}

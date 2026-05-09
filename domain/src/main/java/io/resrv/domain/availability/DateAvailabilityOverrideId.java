package io.resrv.domain.availability;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record DateAvailabilityOverrideId(UUID value) {

    public DateAvailabilityOverrideId {
        Objects.requireNonNull(value, "DateAvailabilityOverrideId must not be null");
    }

    public static DateAvailabilityOverrideId create() {
        return new DateAvailabilityOverrideId(Generators.timeBasedEpochGenerator().generate());
    }

    public static DateAvailabilityOverrideId of(final UUID value) {
        return new DateAvailabilityOverrideId(value);
    }
}

package io.resrv.domain.availability;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record WeeklyAvailabilityId(UUID value) {

    public WeeklyAvailabilityId {
        Objects.requireNonNull(value, "WeeklyAvailabilityId must not be null");
    }

    public static WeeklyAvailabilityId create() {
        return new WeeklyAvailabilityId(Generators.timeBasedEpochGenerator().generate());
    }

    public static WeeklyAvailabilityId of(final UUID value) {
        return new WeeklyAvailabilityId(value);
    }
}

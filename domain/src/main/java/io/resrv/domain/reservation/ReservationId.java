package io.resrv.domain.reservation;

import com.fasterxml.uuid.Generators;
import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "ReservationId must not be null");
    }

    public static ReservationId create() {
        return new ReservationId(Generators.timeBasedEpochGenerator().generate());
    }

    public static ReservationId of(final UUID value) {
        return new ReservationId(value);
    }
}

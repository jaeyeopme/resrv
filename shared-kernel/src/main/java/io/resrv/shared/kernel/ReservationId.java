package io.resrv.shared.kernel;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "Reservation id must not be null");
    }

    public static ReservationId create() {
        return of(UUID.randomUUID());
    }

    public static ReservationId of(final UUID value) {
        return new ReservationId(value);
    }
}

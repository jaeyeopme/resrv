package io.resrv.timeslot.application.reservation.in;

import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record CustomerReservationResult(
        UUID reservationId,
        BusinessSummary business,
        ResourceSummary resource,
        Instant startAt,
        Instant endAt,
        ReservationState state,
        Instant holdExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        ZoneId businessZone) {

    public CustomerReservationResult {
        Objects.requireNonNull(reservationId, "Reservation id must not be null");
        Objects.requireNonNull(business, "Business summary must not be null");
        Objects.requireNonNull(resource, "Resource summary must not be null");
        Objects.requireNonNull(startAt, "Start at must not be null");
        Objects.requireNonNull(endAt, "End at must not be null");
        Objects.requireNonNull(state, "State must not be null");
        Objects.requireNonNull(holdExpiresAt, "Hold expires at must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
        Objects.requireNonNull(businessZone, "Business zone must not be null");
    }

    public record BusinessSummary(UUID id, String name, String slug, String timezone) {

        public BusinessSummary {
            Objects.requireNonNull(id, "Business id must not be null");
            Objects.requireNonNull(name, "Business name must not be null");
            Objects.requireNonNull(slug, "Business slug must not be null");
            Objects.requireNonNull(timezone, "Business timezone must not be null");
        }
    }

    public record ResourceSummary(UUID id, String name, boolean active) {

        public ResourceSummary {
            Objects.requireNonNull(id, "Resource id must not be null");
            Objects.requireNonNull(name, "Resource name must not be null");
        }
    }
}

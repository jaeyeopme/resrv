package io.resrv.timeslot.domain.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import java.util.Objects;

public record Resource(
        ResourceId id,
        BusinessId businessId,
        ResourceName name,
        String description,
        ResourceStatus status,
        ResourceBookingOverrides bookingOverrides,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    public Resource {
        Objects.requireNonNull(id, "Resource id must not be null");
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(name, "Resource name must not be null");
        description = normalizeDescription(description);
        Objects.requireNonNull(status, "Resource status must not be null");
        Objects.requireNonNull(bookingOverrides, "Resource booking overrides must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static Resource create(
            final BusinessId businessId,
            final ResourceName name,
            final String description,
            final ResourceBookingOverrides bookingOverrides,
            final Instant now) {
        return new Resource(
                ResourceId.create(),
                businessId,
                name,
                description,
                ResourceStatus.ACTIVE,
                bookingOverrides,
                now,
                now);
    }

    public static Resource reconstitute(
            final ResourceId id,
            final BusinessId businessId,
            final ResourceName name,
            final String description,
            final ResourceStatus status,
            final ResourceBookingOverrides bookingOverrides,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Resource(
                id, businessId, name, description, status, bookingOverrides, createdAt, updatedAt);
    }

    public Resource deactivate(final Instant now) {
        Objects.requireNonNull(now, "Updated at must not be null");
        return new Resource(
                id,
                businessId,
                name,
                description,
                ResourceStatus.INACTIVE,
                bookingOverrides,
                createdAt,
                now);
    }

    public Resource activate(final Instant now) {
        Objects.requireNonNull(now, "Updated at must not be null");
        return new Resource(
                id,
                businessId,
                name,
                description,
                ResourceStatus.ACTIVE,
                bookingOverrides,
                createdAt,
                now);
    }

    public Resource replaceDetails(
            final ResourceName name,
            final String description,
            final ResourceBookingOverrides bookingOverrides,
            final Instant now) {
        Objects.requireNonNull(now, "Updated at must not be null");
        return new Resource(
                id, businessId, name, description, status, bookingOverrides, createdAt, now);
    }

    public static String normalizeDescription(final String description) {
        if (description == null) {
            return null;
        }
        final var trimmed = description.strip();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Resource description must be 0-500 characters after trimming");
        }
        return trimmed;
    }
}

package io.resrv.timeslot.domain.resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResourceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void createSetsActiveStatusAndNormalizesDescription() {
        final var businessId = BusinessId.create();
        final var overrides =
                new ResourceBookingOverrides(
                        new SlotDuration(30), new HoldTtl(10), new CancellationWindow(120));

        final var resource =
                Resource.create(
                        businessId,
                        new ResourceName(" Room A "),
                        new ResourceSlug("room-a"),
                        "  Window side  ",
                        overrides,
                        NOW);

        assertEquals(businessId, resource.businessId());
        assertEquals("Room A", resource.name().value());
        assertEquals("room-a", resource.slug().value());
        assertEquals("Window side", resource.description());
        assertEquals(ResourceStatus.ACTIVE, resource.status());
        assertEquals(overrides, resource.bookingOverrides());
        assertEquals(NOW, resource.createdAt());
        assertEquals(NOW, resource.updatedAt());
    }

    @Test
    void blankDescriptionBecomesNullAndNoOverridesAreAllowed() {
        final var resource =
                Resource.create(
                        BusinessId.create(),
                        new ResourceName("Room B"),
                        new ResourceSlug("room-b"),
                        "  ",
                        ResourceBookingOverrides.none(),
                        NOW);

        assertNull(resource.description());
        assertEquals(ResourceBookingOverrides.none(), resource.bookingOverrides());
    }

    @Test
    void descriptionAllowsFiveHundredCharactersAfterTrimming() {
        final var description = "x".repeat(500);

        final var resource =
                Resource.create(
                        BusinessId.create(),
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        " " + description + " ",
                        ResourceBookingOverrides.none(),
                        NOW);

        assertEquals(description, resource.description());
    }

    @Test
    void descriptionLongerThanFiveHundredCharactersIsRejectedAfterTrimming() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                Resource.create(
                                        BusinessId.create(),
                                        new ResourceName("Room A"),
                                        new ResourceSlug("room-a"),
                                        "x".repeat(501),
                                        ResourceBookingOverrides.none(),
                                        NOW));

        assertEquals(
                "Resource description must be 0-500 characters after trimming",
                exception.getMessage());
    }

    @Test
    void reconstituteKeepsPersistedValues() {
        final var id = ResourceId.create();
        final var businessId = BusinessId.create();
        final var overrides = ResourceBookingOverrides.none();

        final var resource =
                Resource.reconstitute(
                        id,
                        businessId,
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        null,
                        ResourceStatus.INACTIVE,
                        overrides,
                        NOW,
                        LATER);

        assertEquals(id, resource.id());
        assertEquals(businessId, resource.businessId());
        assertNull(resource.description());
        assertEquals(ResourceStatus.INACTIVE, resource.status());
        assertEquals(overrides, resource.bookingOverrides());
        assertEquals(NOW, resource.createdAt());
        assertEquals(LATER, resource.updatedAt());
    }

    @Test
    void deactivateReturnsInactiveResourceWithUpdatedTimestampOnly() {
        final var resource =
                Resource.create(
                        BusinessId.create(),
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        "Window side",
                        ResourceBookingOverrides.none(),
                        NOW);

        final var deactivated = resource.deactivate(LATER);

        assertEquals(resource.id(), deactivated.id());
        assertEquals(resource.businessId(), deactivated.businessId());
        assertEquals(resource.name(), deactivated.name());
        assertEquals(resource.slug(), deactivated.slug());
        assertEquals(resource.description(), deactivated.description());
        assertEquals(resource.bookingOverrides(), deactivated.bookingOverrides());
        assertEquals(ResourceStatus.INACTIVE, deactivated.status());
        assertEquals(NOW, deactivated.createdAt());
        assertEquals(LATER, deactivated.updatedAt());
    }

    @Test
    void invalidNameIsRejected() {
        assertThrows(NullPointerException.class, () -> new ResourceName(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceName(" "));
        assertThrows(IllegalArgumentException.class, () -> new ResourceName("a".repeat(101)));
    }

    @Test
    void invalidSlugIsRejected() {
        assertDoesNotThrow(() -> new ResourceSlug("abc"));
        assertDoesNotThrow(() -> new ResourceSlug("a".repeat(63)));
        assertThrows(NullPointerException.class, () -> new ResourceSlug(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("ab"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("Room-A"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("-room"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("room-"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("room--a"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("a".repeat(64)));
    }

    @Test
    void nullRequiredResourceValuesAreRejected() {
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                BusinessId.create(),
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                null,
                                ResourceStatus.ACTIVE,
                                null,
                                NOW,
                                NOW));
    }
}

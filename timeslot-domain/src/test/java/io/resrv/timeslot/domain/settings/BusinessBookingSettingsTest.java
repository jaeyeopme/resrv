package io.resrv.timeslot.domain.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BusinessBookingSettingsTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void createStoresSettingsAndTimestamps() {
        final var businessId = BusinessId.create();

        final var settings =
                BusinessBookingSettings.create(
                        businessId,
                        new SlotDuration(30),
                        new HoldTtl(10),
                        new CancellationWindow(120),
                        new MaxAdvanceBookingDays(90),
                        CREATED_AT);

        assertEquals(businessId, settings.businessId());
        assertEquals(new SlotDuration(30), settings.slotDuration());
        assertEquals(new HoldTtl(10), settings.holdTtl());
        assertEquals(new CancellationWindow(120), settings.cancellationWindow());
        assertEquals(new MaxAdvanceBookingDays(90), settings.maxAdvanceBookingDays());
        assertEquals(CREATED_AT, settings.createdAt());
        assertEquals(CREATED_AT, settings.updatedAt());
    }

    @Test
    void reconstitutePreservesValues() {
        final var businessId = BusinessId.create();

        final var settings =
                BusinessBookingSettings.reconstitute(
                        businessId,
                        new SlotDuration(45),
                        new HoldTtl(15),
                        new CancellationWindow(0),
                        new MaxAdvanceBookingDays(30),
                        CREATED_AT,
                        UPDATED_AT);

        assertEquals(businessId, settings.businessId());
        assertEquals(new SlotDuration(45), settings.slotDuration());
        assertEquals(new HoldTtl(15), settings.holdTtl());
        assertEquals(new CancellationWindow(0), settings.cancellationWindow());
        assertEquals(new MaxAdvanceBookingDays(30), settings.maxAdvanceBookingDays());
        assertEquals(CREATED_AT, settings.createdAt());
        assertEquals(UPDATED_AT, settings.updatedAt());
    }

    @Test
    void updatePreservesBusinessIdAndCreatedAt() {
        final var businessId = BusinessId.create();
        final var existing =
                BusinessBookingSettings.create(
                        businessId,
                        new SlotDuration(30),
                        new HoldTtl(10),
                        new CancellationWindow(120),
                        new MaxAdvanceBookingDays(90),
                        CREATED_AT);

        final var updated =
                existing.update(
                        new SlotDuration(60),
                        new HoldTtl(5),
                        new CancellationWindow(240),
                        new MaxAdvanceBookingDays(180),
                        UPDATED_AT);

        assertEquals(businessId, updated.businessId());
        assertEquals(new SlotDuration(60), updated.slotDuration());
        assertEquals(new HoldTtl(5), updated.holdTtl());
        assertEquals(new CancellationWindow(240), updated.cancellationWindow());
        assertEquals(new MaxAdvanceBookingDays(180), updated.maxAdvanceBookingDays());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(UPDATED_AT, updated.updatedAt());
    }

    @Test
    void valueObjectsRejectInvalidValues() {
        final var slotException =
                assertThrows(IllegalArgumentException.class, () -> new SlotDuration(7));
        final var holdException =
                assertThrows(IllegalArgumentException.class, () -> new HoldTtl(31));
        final var cancellationException =
                assertThrows(IllegalArgumentException.class, () -> new CancellationWindow(-1));
        final var advanceException =
                assertThrows(IllegalArgumentException.class, () -> new MaxAdvanceBookingDays(366));

        assertEquals(
                "Slot duration must be 5-480 minutes in 5 minute increments",
                slotException.getMessage());
        assertEquals("Hold TTL must be 1-30 minutes", holdException.getMessage());
        assertEquals(
                "Cancellation window must be 0-10080 minutes", cancellationException.getMessage());
        assertEquals("Max advance booking days must be 1-365", advanceException.getMessage());
    }

    @Test
    void nullSettingsRejected() {
        final var businessId = BusinessId.create();
        final var exception =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                BusinessBookingSettings.create(
                                        businessId,
                                        null,
                                        new HoldTtl(10),
                                        new CancellationWindow(120),
                                        new MaxAdvanceBookingDays(90),
                                        CREATED_AT));

        assertEquals("Slot duration must not be null", exception.getMessage());
    }
}

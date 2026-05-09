package io.resrv.domain.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class WeeklyAvailabilityTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2025-01-02T00:00:00Z");

    @Test
    void createStoresRangeAndTimestamps() {
        final var availability =
                WeeklyAvailability.create(
                        TENANT_ID,
                        RESOURCE_ID,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        CREATED_AT);

        assertNotNull(availability.id());
        assertEquals(TENANT_ID, availability.tenantId());
        assertEquals(RESOURCE_ID, availability.resourceId());
        assertEquals(DayOfWeek.MONDAY, availability.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), availability.startTime());
        assertEquals(LocalTime.of(18, 0), availability.endTime());
        assertEquals(CREATED_AT, availability.createdAt());
        assertEquals(CREATED_AT, availability.updatedAt());
    }

    @Test
    void updatePreservesIdentityAndCreationTime() {
        final var original =
                WeeklyAvailability.create(
                        TENANT_ID,
                        RESOURCE_ID,
                        DayOfWeek.TUESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        CREATED_AT);

        final var updated = original.update(LocalTime.of(10, 0), LocalTime.of(16, 0), UPDATED_AT);

        assertEquals(original.id(), updated.id());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(LocalTime.of(10, 0), updated.startTime());
        assertEquals(LocalTime.of(16, 0), updated.endTime());
        assertEquals(UPDATED_AT, updated.updatedAt());
    }

    @Test
    void rejectsInvalidRange() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        WeeklyAvailability.create(
                                TENANT_ID,
                                RESOURCE_ID,
                                DayOfWeek.WEDNESDAY,
                                LocalTime.of(12, 0),
                                LocalTime.of(12, 0),
                                CREATED_AT));
    }
}

package io.resrv.domain.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DateAvailabilityOverrideTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final LocalDate DATE = LocalDate.of(2025, 1, 5);
    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2025-01-02T00:00:00Z");

    @Test
    void closedOverrideHasNoRange() {
        final var override =
                DateAvailabilityOverride.closed(TENANT_ID, RESOURCE_ID, DATE, CREATED_AT);

        assertNotNull(override.id());
        assertEquals(TENANT_ID, override.tenantId());
        assertEquals(RESOURCE_ID, override.resourceId());
        assertEquals(DATE, override.date());
        assertTrue(override.closed());
        assertNull(override.startTime());
        assertNull(override.endTime());
        assertEquals(CREATED_AT, override.createdAt());
        assertEquals(CREATED_AT, override.updatedAt());
    }

    @Test
    void openOverrideStoresRangeAndCanCloseAgain() {
        final var open =
                DateAvailabilityOverride.open(
                        TENANT_ID,
                        RESOURCE_ID,
                        DATE,
                        LocalTime.of(10, 0),
                        LocalTime.of(15, 0),
                        CREATED_AT);

        final var closed = open.updateClosed(UPDATED_AT);

        assertFalse(open.closed());
        assertEquals(LocalTime.of(10, 0), open.startTime());
        assertEquals(LocalTime.of(15, 0), open.endTime());
        assertEquals(open.id(), closed.id());
        assertTrue(closed.closed());
        assertNull(closed.startTime());
        assertNull(closed.endTime());
        assertEquals(CREATED_AT, closed.createdAt());
        assertEquals(UPDATED_AT, closed.updatedAt());
    }

    @Test
    void updateOpenPreservesIdentityAndCreationTime() {
        final var closed =
                DateAvailabilityOverride.closed(TENANT_ID, RESOURCE_ID, DATE, CREATED_AT);

        final var open = closed.updateOpen(LocalTime.of(11, 0), LocalTime.of(14, 0), UPDATED_AT);

        assertEquals(closed.id(), open.id());
        assertFalse(open.closed());
        assertEquals(LocalTime.of(11, 0), open.startTime());
        assertEquals(LocalTime.of(14, 0), open.endTime());
        assertEquals(CREATED_AT, open.createdAt());
        assertEquals(UPDATED_AT, open.updatedAt());
    }

    @Test
    void rejectsInvalidOpenRange() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DateAvailabilityOverride.open(
                                TENANT_ID,
                                RESOURCE_ID,
                                DATE,
                                LocalTime.of(15, 0),
                                LocalTime.of(10, 0),
                                CREATED_AT));
    }
}

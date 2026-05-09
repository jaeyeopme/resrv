package io.resrv.application.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityCommand;
import io.resrv.application.availability.out.DateAvailabilityOverrideCommandPort;
import io.resrv.application.availability.out.DateAvailabilityOverrideQueryPort;
import io.resrv.application.availability.out.WeeklyAvailabilityCommandPort;
import io.resrv.application.availability.out.WeeklyAvailabilityQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.DateAvailabilityOverrideId;
import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.availability.WeeklyAvailabilityId;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AvailabilityServiceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final LocalDate OVERRIDE_DATE = LocalDate.of(2025, 1, 6);

    private ResourceQueryPort resourceQueryPort;
    private WeeklyAvailabilityCommandPort weeklyCommandPort;
    private WeeklyAvailabilityQueryPort weeklyQueryPort;
    private DateAvailabilityOverrideCommandPort overrideCommandPort;
    private DateAvailabilityOverrideQueryPort overrideQueryPort;
    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        resourceQueryPort = mock(ResourceQueryPort.class);
        weeklyCommandPort = mock(WeeklyAvailabilityCommandPort.class);
        weeklyQueryPort = mock(WeeklyAvailabilityQueryPort.class);
        overrideCommandPort = mock(DateAvailabilityOverrideCommandPort.class);
        overrideQueryPort = mock(DateAvailabilityOverrideQueryPort.class);
        service =
                new AvailabilityService(
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        resourceQueryPort,
                        weeklyCommandPort,
                        weeklyQueryPort,
                        overrideCommandPort,
                        overrideQueryPort);
    }

    @Test
    void upsertWeekly_createsAvailabilityForActiveResource() {
        stubResource(ResourceStatus.ACTIVE);
        when(weeklyQueryPort.findByTenantIdAndResourceIdAndDayOfWeek(
                        TENANT_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        final var result =
                service.upsert(
                        new UpsertWeeklyAvailabilityCommand(
                                TENANT_ID,
                                RESOURCE_ID,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0)));

        assertEquals(TENANT_ID.value(), result.tenantId());
        assertEquals(RESOURCE_ID.value(), result.resourceId());
        assertEquals(DayOfWeek.MONDAY, result.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), result.startTime());
        assertEquals(FIXED_NOW, result.createdAt());
        assertEquals(FIXED_NOW, result.updatedAt());
        verify(weeklyCommandPort).save(any(WeeklyAvailability.class));
    }

    @Test
    void upsertWeekly_updatesExistingAvailability() {
        stubResource(ResourceStatus.ACTIVE);
        final var existing =
                WeeklyAvailability.reconstitute(
                        WeeklyAvailabilityId.create(),
                        TENANT_ID,
                        RESOURCE_ID,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(15, 0),
                        CREATED_AT,
                        CREATED_AT);
        when(weeklyQueryPort.findByTenantIdAndResourceIdAndDayOfWeek(
                        TENANT_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(existing));

        final var result =
                service.upsert(
                        new UpsertWeeklyAvailabilityCommand(
                                TENANT_ID,
                                RESOURCE_ID,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0)));

        assertEquals(existing.id().value(), result.id());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(FIXED_NOW, result.updatedAt());
        assertEquals(LocalTime.of(9, 0), result.startTime());

        final var captor = ArgumentCaptor.forClass(WeeklyAvailability.class);
        verify(weeklyCommandPort).save(captor.capture());
        assertEquals(existing.id(), captor.getValue().id());
        assertEquals(LocalTime.of(17, 0), captor.getValue().endTime());
    }

    @Test
    void upsertOpenOverride_createsDateSpecificWindow() {
        stubResource(ResourceStatus.ACTIVE);
        when(overrideQueryPort.findByTenantIdAndResourceIdAndDate(
                        TENANT_ID, RESOURCE_ID, OVERRIDE_DATE))
                .thenReturn(Optional.empty());

        final var result =
                service.upsert(
                        new UpsertDateAvailabilityOverrideCommand(
                                TENANT_ID,
                                RESOURCE_ID,
                                OVERRIDE_DATE,
                                false,
                                LocalTime.of(12, 0),
                                LocalTime.of(18, 0)));

        assertFalse(result.closed());
        assertEquals(LocalTime.of(12, 0), result.startTime());
        assertEquals(LocalTime.of(18, 0), result.endTime());
        assertEquals(FIXED_NOW, result.createdAt());
        verify(overrideCommandPort).save(any(DateAvailabilityOverride.class));
    }

    @Test
    void upsertClosedOverride_updatesExistingOpenOverride() {
        stubResource(ResourceStatus.ACTIVE);
        final var existing =
                DateAvailabilityOverride.reconstitute(
                        DateAvailabilityOverrideId.create(),
                        TENANT_ID,
                        RESOURCE_ID,
                        OVERRIDE_DATE,
                        false,
                        LocalTime.of(10, 0),
                        LocalTime.of(14, 0),
                        CREATED_AT,
                        CREATED_AT);
        when(overrideQueryPort.findByTenantIdAndResourceIdAndDate(
                        TENANT_ID, RESOURCE_ID, OVERRIDE_DATE))
                .thenReturn(Optional.of(existing));

        final var result =
                service.upsert(
                        new UpsertDateAvailabilityOverrideCommand(
                                TENANT_ID, RESOURCE_ID, OVERRIDE_DATE, true, null, null));

        assertTrue(result.closed());
        assertNull(result.startTime());
        assertNull(result.endTime());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(FIXED_NOW, result.updatedAt());
        verify(overrideCommandPort).save(any(DateAvailabilityOverride.class));
    }

    @Test
    void deleteWeekly_validatesResourceBeforeDeleting() {
        stubResource(ResourceStatus.ACTIVE);

        service.delete(
                new DeleteWeeklyAvailabilityCommand(TENANT_ID, RESOURCE_ID, DayOfWeek.MONDAY));

        verify(weeklyCommandPort)
                .deleteByTenantIdAndResourceIdAndDayOfWeek(
                        TENANT_ID, RESOURCE_ID, DayOfWeek.MONDAY);
    }

    @Test
    void deleteDateOverride_validatesResourceBeforeDeleting() {
        stubResource(ResourceStatus.ACTIVE);

        service.delete(
                new DeleteDateAvailabilityOverrideCommand(TENANT_ID, RESOURCE_ID, OVERRIDE_DATE));

        verify(overrideCommandPort)
                .deleteByTenantIdAndResourceIdAndDate(TENANT_ID, RESOURCE_ID, OVERRIDE_DATE);
    }

    @Test
    void upsertWeekly_inactiveResource_throwsAndDoesNotSave() {
        stubResource(ResourceStatus.INACTIVE);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.upsert(
                                new UpsertWeeklyAvailabilityCommand(
                                        TENANT_ID,
                                        RESOURCE_ID,
                                        DayOfWeek.MONDAY,
                                        LocalTime.of(9, 0),
                                        LocalTime.of(17, 0))));

        verify(weeklyCommandPort, never()).save(any());
        verify(overrideCommandPort, never()).save(any());
    }

    private void stubResource(final ResourceStatus status) {
        when(resourceQueryPort.findByTenantIdAndId(TENANT_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource(status)));
    }

    private static Resource resource(final ResourceStatus status) {
        return Resource.reconstitute(
                RESOURCE_ID,
                TENANT_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                ResourceDescription.empty(),
                status,
                CREATED_AT,
                CREATED_AT);
    }
}

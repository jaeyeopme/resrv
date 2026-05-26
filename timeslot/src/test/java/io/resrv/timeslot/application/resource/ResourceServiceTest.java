package io.resrv.timeslot.application.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResourceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-02T00:00:00Z");
    private static final BusinessId BUSINESS_ID = BusinessId.create();

    private BusinessBookingSettingsQueryPort settingsQueryPort;
    private BusinessLookupPort businessLookupPort;
    private ResourceCommandPort commandPort;
    private ResourceQueryPort queryPort;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        settingsQueryPort = mock(BusinessBookingSettingsQueryPort.class);
        businessLookupPort = mock(BusinessLookupPort.class);
        commandPort = mock(ResourceCommandPort.class);
        queryPort = mock(ResourceQueryPort.class);
        service =
                new ResourceService(
                        settingsQueryPort,
                        businessLookupPort,
                        commandPort,
                        queryPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requiresBusinessBookingSettingsBeforeResourceCreation() {
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        final var exception =
                assertThrows(
                        BookingSettingsRequiredException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                "Room A",
                                                "room-a",
                                                null,
                                                null,
                                                null,
                                                null)));

        assertEquals(
                "Booking settings are required for business: " + BUSINESS_ID.value(),
                exception.getMessage());
        verify(queryPort, never()).findByBusinessIdAndSlug(any(), any());
        verify(commandPort, never()).save(any());
    }

    @Test
    void createsResourceWithOverrides() {
        final var slug = new ResourceSlug("room-a");
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID))
                .thenReturn(Optional.of(existingSettings()));
        when(queryPort.findByBusinessIdAndSlug(BUSINESS_ID, slug)).thenReturn(Optional.empty());

        final var result =
                service.create(
                        new CreateResourceCommand(
                                BUSINESS_ID, " Room A ", "room-a", "  Window side  ", 30, 10, 120));

        assertEquals(BUSINESS_ID.value(), result.businessId());
        assertEquals("Room A", result.name());
        assertEquals("room-a", result.slug());
        assertEquals("Window side", result.description());
        assertEquals(ResourceStatus.ACTIVE, result.status());
        assertEquals(30, result.slotDurationMinutes());
        assertEquals(10, result.holdTtlMinutes());
        assertEquals(120, result.cancellationWindowMinutes());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        final var captor = ArgumentCaptor.forClass(Resource.class);
        verify(commandPort).save(captor.capture());
        final var saved = captor.getValue();
        assertEquals(saved.id().value(), result.id());
        assertEquals(BUSINESS_ID, saved.businessId());
        assertEquals(new ResourceName("Room A"), saved.name());
        assertEquals(slug, saved.slug());
        assertEquals("Window side", saved.description());
        assertEquals(ResourceStatus.ACTIVE, saved.status());
        assertEquals(new SlotDuration(30), saved.bookingOverrides().slotDuration());
        assertEquals(new HoldTtl(10), saved.bookingOverrides().holdTtl());
        assertEquals(new CancellationWindow(120), saved.bookingOverrides().cancellationWindow());
    }

    @Test
    void createsResourceWithNoOverrides() {
        final var slug = new ResourceSlug("room-b");
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID))
                .thenReturn(Optional.of(existingSettings()));
        when(queryPort.findByBusinessIdAndSlug(BUSINESS_ID, slug)).thenReturn(Optional.empty());

        final var result =
                service.create(
                        new CreateResourceCommand(
                                BUSINESS_ID, "Room B", "room-b", "  ", null, null, null));

        assertNull(result.description());
        assertNull(result.slotDurationMinutes());
        assertNull(result.holdTtlMinutes());
        assertNull(result.cancellationWindowMinutes());

        final var captor = ArgumentCaptor.forClass(Resource.class);
        verify(commandPort).save(captor.capture());
        final var saved = captor.getValue();
        assertEquals(ResourceBookingOverrides.none(), saved.bookingOverrides());
        assertNull(saved.description());
    }

    @Test
    void duplicateSlugThrowsAndDoesNotSave() {
        final var slug = new ResourceSlug("room-a");
        final var existing =
                Resource.create(
                        BUSINESS_ID,
                        new ResourceName("Room A"),
                        slug,
                        null,
                        ResourceBookingOverrides.none(),
                        NOW);
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID))
                .thenReturn(Optional.of(existingSettings()));
        when(queryPort.findByBusinessIdAndSlug(BUSINESS_ID, slug))
                .thenReturn(Optional.of(existing));

        final var exception =
                assertThrows(
                        ResourceSlugAlreadyExistsException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                "Room A",
                                                "room-a",
                                                null,
                                                null,
                                                null,
                                                null)));

        assertEquals(BUSINESS_ID, exception.businessId());
        assertEquals(slug, exception.slug());
        assertEquals(
                "Resource slug already exists for business " + BUSINESS_ID.value() + ": room-a",
                exception.getMessage());
        verify(commandPort, never()).save(any());
    }

    @Test
    void nullCommandFailsBeforePorts() {
        final var exception = assertThrows(NullPointerException.class, () -> service.create(null));

        assertEquals("Command must not be null", exception.getMessage());
        verifyNoPorts();
    }

    @Test
    void listActiveReturnsEmptyWhenBusinessIsInactive() {
        when(businessLookupPort.findActiveById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertEquals(List.of(), service.listActive(BUSINESS_ID));

        verifyNoInteractions(queryPort);
    }

    @Test
    void listActiveReturnsActiveResourcesWhenBusinessIsActive() {
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(
                        Optional.of(
                                new BusinessLookupPort.BusinessView(
                                        BUSINESS_ID,
                                        "Salon A",
                                        "salon-a",
                                        Timezone.of("Asia/Seoul"))));
        when(queryPort.findActiveByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(resource(ResourceBookingOverrides.none())));

        final var resources = service.listActive(BUSINESS_ID);

        assertEquals(1, resources.size());
        assertEquals("Room A", resources.getFirst().name());
    }

    @Test
    void invalidNameFailsBeforePorts() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                " ",
                                                "room-a",
                                                null,
                                                null,
                                                null,
                                                null)));

        assertEquals("Resource name must be 1-100 characters", exception.getMessage());
        verifyNoPorts();
    }

    @Test
    void invalidSlugFailsBeforePorts() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                "Room A",
                                                "room--a",
                                                null,
                                                null,
                                                null,
                                                null)));

        assertEquals("Resource slug must be 3-63 lowercase URL characters", exception.getMessage());
        verifyNoPorts();
    }

    @Test
    void invalidOverridePrimitiveFailsBeforePorts() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                "Room A",
                                                "room-a",
                                                null,
                                                7,
                                                null,
                                                null)));

        assertEquals(
                "Slot duration must be 5-480 minutes in 5 minute increments",
                exception.getMessage());
        verifyNoPorts();
    }

    @Test
    void descriptionLongerThanFiveHundredCharactersFailsBeforePorts() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.create(
                                        new CreateResourceCommand(
                                                BUSINESS_ID,
                                                "Room A",
                                                "room-a",
                                                "x".repeat(501),
                                                null,
                                                null,
                                                null)));

        assertEquals(
                "Resource description must be 0-500 characters after trimming",
                exception.getMessage());
        verifyNoPorts();
    }

    @Test
    void deactivateReturnsInactiveResourceWithoutChangingIdentityOrOverrides() {
        final var overrides =
                new ResourceBookingOverrides(
                        new SlotDuration(30), new HoldTtl(10), new CancellationWindow(120));
        final var resource =
                Resource.create(
                        BUSINESS_ID,
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        "Window side",
                        overrides,
                        NOW);

        final var deactivated = resource.deactivate(LATER);

        assertEquals(resource.id(), deactivated.id());
        assertEquals(resource.businessId(), deactivated.businessId());
        assertEquals(resource.name(), deactivated.name());
        assertEquals(resource.slug(), deactivated.slug());
        assertEquals(resource.description(), deactivated.description());
        assertEquals(overrides, deactivated.bookingOverrides());
        assertEquals(ResourceStatus.INACTIVE, deactivated.status());
        assertEquals(NOW, deactivated.createdAt());
        assertEquals(LATER, deactivated.updatedAt());
    }

    private static BusinessBookingSettings existingSettings() {
        return BusinessBookingSettings.create(
                BUSINESS_ID,
                new SlotDuration(30),
                new HoldTtl(10),
                new CancellationWindow(120),
                new MaxAdvanceBookingDays(90),
                NOW);
    }

    private static Resource resource(final ResourceBookingOverrides overrides) {
        return Resource.reconstitute(
                ResourceId.create(),
                BUSINESS_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                null,
                ResourceStatus.ACTIVE,
                overrides,
                NOW,
                NOW);
    }

    private void verifyNoPorts() {
        verifyNoInteractions(settingsQueryPort, businessLookupPort, queryPort, commandPort);
    }
}

package io.resrv.timeslot.application.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.discovery.in.HoldReservationByBusinessSlugCommand;
import io.resrv.timeslot.application.discovery.in.PublicBusinessDiscoveryQuery;
import io.resrv.timeslot.application.discovery.in.PublicResourceDiscoveryQuery;
import io.resrv.timeslot.application.discovery.in.PublicSlotDiscoveryQuery;
import io.resrv.timeslot.application.reservation.ReservationService;
import io.resrv.timeslot.application.reservation.SlotUnavailableException;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class PublicBookingDiscoveryServiceTest {

    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final BusinessId OTHER_BUSINESS_ID = BusinessId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final Timezone TIMEZONE = Timezone.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-05-24T23:30:00Z");
    private static final LocalDate DATE = LocalDate.parse("2026-05-25");

    private BusinessLookupPort businessLookupPort;
    private BusinessBookingSettingsQueryPort settingsQueryPort;
    private ResourceQueryPort resourceQueryPort;
    private ResourceScheduleQueryPort scheduleQueryPort;
    private ReservationQueryPort reservationQueryPort;
    private ReservationService reservationService;
    private PublicBookingDiscoveryService service;

    @BeforeEach
    void setUp() {
        businessLookupPort = mock(BusinessLookupPort.class);
        settingsQueryPort = mock(BusinessBookingSettingsQueryPort.class);
        resourceQueryPort = mock(ResourceQueryPort.class);
        scheduleQueryPort = mock(ResourceScheduleQueryPort.class);
        reservationQueryPort = mock(ReservationQueryPort.class);
        reservationService = mock(ReservationService.class);
        final var resolver =
                new PublicBookableBusinessResolver(businessLookupPort, settingsQueryPort);
        service =
                new PublicBookingDiscoveryService(
                        resolver,
                        resourceQueryPort,
                        scheduleQueryPort,
                        reservationQueryPort,
                        reservationService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void discoversBusinessWithoutInternalBusinessId() {
        givenBookableBusiness();

        final var result = service.discoverBusiness(new PublicBusinessDiscoveryQuery("salon-a"));

        assertEquals("salon-a", result.slug());
        assertEquals("Salon A", result.name());
        assertEquals("Asia/Seoul", result.timezone());
    }

    @Test
    void rejectsMissingInactiveAndMissingSettingsBusinessesWithSameException() {
        when(businessLookupPort.findActiveBySlug("missing")).thenReturn(Optional.empty());
        when(businessLookupPort.findCurrentSummaryBySlug("missing")).thenReturn(Optional.empty());
        when(businessLookupPort.findActiveBySlug("inactive")).thenReturn(Optional.empty());
        when(businessLookupPort.findCurrentSummaryBySlug("inactive"))
                .thenReturn(Optional.of(business()));
        when(businessLookupPort.findActiveBySlug("not-bookable"))
                .thenReturn(Optional.of(business()));
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        final var missing =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.discoverBusiness(
                                        new PublicBusinessDiscoveryQuery("missing")));
        final var inactive =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.discoverBusiness(
                                        new PublicBusinessDiscoveryQuery("inactive")));
        final var notBookable =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.discoverBusiness(
                                        new PublicBusinessDiscoveryQuery("not-bookable")));

        assertEquals(missing.getMessage(), inactive.getMessage());
        assertEquals(missing.getMessage(), notBookable.getMessage());
    }

    @Test
    void listsOnlyActiveResourcesForBookableBusiness() {
        givenBookableBusiness();
        when(resourceQueryPort.findActiveByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(activeResource()));

        final var resources = service.listResources(new PublicResourceDiscoveryQuery("salon-a"));

        assertEquals(1, resources.size());
        assertEquals(RESOURCE_ID.value(), resources.getFirst().resourceId());
        assertEquals("salon-a", resources.getFirst().businessSlug());
    }

    @Test
    void resourceDenialsUseSamePublicException() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID)).thenReturn(Optional.empty());
        final var missing =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.listSlots(
                                        new PublicSlotDiscoveryQuery(
                                                "salon-a", RESOURCE_ID, DATE)));

        when(resourceQueryPort.findById(RESOURCE_ID))
                .thenReturn(Optional.of(inactiveResource(BUSINESS_ID)));
        final var inactive =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.listSlots(
                                        new PublicSlotDiscoveryQuery(
                                                "salon-a", RESOURCE_ID, DATE)));

        when(resourceQueryPort.findById(RESOURCE_ID))
                .thenReturn(Optional.of(activeResource(OTHER_BUSINESS_ID)));
        final var wrongBusiness =
                assertThrows(
                        PublicDiscoveryNotFoundException.class,
                        () ->
                                service.listSlots(
                                        new PublicSlotDiscoveryQuery(
                                                "salon-a", RESOURCE_ID, DATE)));

        assertEquals(missing.getMessage(), inactive.getMessage());
        assertEquals(missing.getMessage(), wrongBusiness.getMessage());
    }

    @Test
    void marksBlockedSlotsUnavailableAndKeepsSlotIds() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID)).thenReturn(Optional.of(activeResource()));
        when(scheduleQueryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, DATE))
                .thenReturn(Optional.empty());
        when(scheduleQueryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(
                        Optional.of(
                                WeeklyResourceSchedule.create(
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        DayOfWeek.MONDAY,
                                        List.of(
                                                new ScheduleWindow(
                                                        LocalTime.of(9, 0), LocalTime.of(10, 0))),
                                        NOW)));
        when(reservationQueryPort.findActiveBlockers(any(), any(), any(), any(), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(blocker()));

        final var slots =
                service.listSlots(new PublicSlotDiscoveryQuery("salon-a", RESOURCE_ID, DATE));

        assertEquals(2, slots.size());
        assertFalse(slots.getFirst().slotId().isBlank());
        assertTrue(slots.getFirst().available());
        assertFalse(slots.get(1).available());
    }

    @Test
    void delegatesSlugScopedHoldWithResolvedBusinessId() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID)).thenReturn(Optional.of(activeResource()));

        service.holdReservation(
                new HoldReservationByBusinessSlugCommand(
                        "salon-a", RESOURCE_ID, ACCOUNT_ID, "slot"));

        final var captor = ArgumentCaptor.forClass(HoldReservationCommand.class);
        verify(reservationService).hold(captor.capture());
        assertEquals(BUSINESS_ID, captor.getValue().businessId());
        assertEquals(RESOURCE_ID, captor.getValue().resourceId());
        assertEquals(ACCOUNT_ID, captor.getValue().accountId());
    }

    @Test
    void rejectsInactiveResourceBeforeSlugScopedHoldDelegation() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID))
                .thenReturn(Optional.of(inactiveResource(BUSINESS_ID)));

        assertThrows(
                PublicDiscoveryNotFoundException.class,
                () ->
                        service.holdReservation(
                                new HoldReservationByBusinessSlugCommand(
                                        "salon-a", RESOURCE_ID, ACCOUNT_ID, "slot")));
        verifyNoInteractions(reservationService);
    }

    @Test
    void rejectsWrongBusinessResourceBeforeSlugScopedHoldDelegation() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID))
                .thenReturn(Optional.of(activeResource(OTHER_BUSINESS_ID)));

        assertThrows(
                PublicDiscoveryNotFoundException.class,
                () ->
                        service.holdReservation(
                                new HoldReservationByBusinessSlugCommand(
                                        "salon-a", RESOURCE_ID, ACCOUNT_ID, "slot")));
        verifyNoInteractions(reservationService);
    }

    @Test
    void delegatesSlotIdentityAndAvailabilityRevalidationToReservationService() {
        givenBookableBusiness();
        when(resourceQueryPort.findById(RESOURCE_ID)).thenReturn(Optional.of(activeResource()));
        when(reservationService.hold(any(HoldReservationCommand.class)))
                .thenThrow(new SlotUnavailableException(RESOURCE_ID, NOW));
        final var rejectedSlotIds =
                List.of(
                        "unavailable-slot",
                        "malformed-slot",
                        "stale-slot",
                        "wrong-business-slot",
                        "wrong-resource-slot");

        for (final var slotId : rejectedSlotIds) {
            assertThrows(
                    SlotUnavailableException.class,
                    () ->
                            service.holdReservation(
                                    new HoldReservationByBusinessSlugCommand(
                                            "salon-a", RESOURCE_ID, ACCOUNT_ID, slotId)));
        }

        final var captor = ArgumentCaptor.forClass(HoldReservationCommand.class);
        verify(reservationService, times(rejectedSlotIds.size())).hold(captor.capture());
        assertEquals(
                rejectedSlotIds,
                captor.getAllValues().stream().map(HoldReservationCommand::slotId).toList());
    }

    private void givenBookableBusiness() {
        when(businessLookupPort.findActiveBySlug("salon-a")).thenReturn(Optional.of(business()));
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(settings()));
    }

    private static BusinessLookupPort.BusinessView business() {
        return new BusinessLookupPort.BusinessView(BUSINESS_ID, "Salon A", "salon-a", TIMEZONE);
    }

    private static BusinessBookingSettings settings() {
        return BusinessBookingSettings.create(
                BUSINESS_ID,
                new SlotDuration(30),
                new HoldTtl(10),
                new CancellationWindow(60),
                new MaxAdvanceBookingDays(30),
                NOW);
    }

    private static Resource activeResource() {
        return activeResource(BUSINESS_ID);
    }

    private static Resource activeResource(final BusinessId businessId) {
        return resource(businessId, ResourceStatus.ACTIVE);
    }

    private static Resource inactiveResource(final BusinessId businessId) {
        return resource(businessId, ResourceStatus.INACTIVE);
    }

    private static Resource resource(final BusinessId businessId, final ResourceStatus status) {
        return Resource.reconstitute(
                RESOURCE_ID,
                businessId,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                null,
                status,
                ResourceBookingOverrides.none(),
                NOW,
                NOW);
    }

    private static Reservation blocker() {
        return Reservation.hold(
                BUSINESS_ID,
                RESOURCE_ID,
                ACCOUNT_ID,
                Instant.parse("2026-05-25T00:30:00Z"),
                Instant.parse("2026-05-25T01:00:00Z"),
                NOW.plusSeconds(60),
                NOW);
    }
}

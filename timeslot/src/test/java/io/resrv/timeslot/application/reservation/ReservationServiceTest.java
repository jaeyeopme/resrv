package io.resrv.timeslot.application.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.lock.out.SlotLockPort;
import io.resrv.timeslot.application.reservation.in.CancelReservationCommand;
import io.resrv.timeslot.application.reservation.in.CheckInReservationCommand;
import io.resrv.timeslot.application.reservation.in.ConfirmReservationCommand;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.in.ListBusinessReservationsQuery;
import io.resrv.timeslot.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReleaseReservationCommand;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import io.resrv.timeslot.domain.reservation.ReservationHoldExpiredException;
import io.resrv.timeslot.domain.reservation.ReservationInvalidStateException;
import io.resrv.timeslot.domain.reservation.ReservationState;
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
import io.resrv.timeslot.domain.slot.SlotId;
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

final class ReservationServiceTest {

    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final AccountId CUSTOMER_ACCOUNT_ID = AccountId.create();
    private static final AccountId STAFF_ACCOUNT_ID = AccountId.create();
    private static final Timezone TIMEZONE = Timezone.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-05-24T23:30:00Z");
    private static final Instant SLOT_START_AT = Instant.parse("2026-05-25T00:00:00Z");
    private static final Instant SLOT_END_AT = Instant.parse("2026-05-25T00:30:00Z");
    private static final LocalDate SLOT_DATE = LocalDate.parse("2026-05-25");

    private BusinessLookupPort businessLookupPort;
    private BusinessBookingSettingsQueryPort settingsQueryPort;
    private ResourceQueryPort resourceQueryPort;
    private ResourceScheduleQueryPort scheduleQueryPort;
    private SlotLockPort slotLockPort;
    private ReservationCommandPort reservationCommandPort;
    private ReservationQueryPort reservationQueryPort;
    private BusinessAccessPort businessAccessPort;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        businessLookupPort = mock(BusinessLookupPort.class);
        settingsQueryPort = mock(BusinessBookingSettingsQueryPort.class);
        resourceQueryPort = mock(ResourceQueryPort.class);
        scheduleQueryPort = mock(ResourceScheduleQueryPort.class);
        slotLockPort = mock(SlotLockPort.class);
        reservationCommandPort = mock(ReservationCommandPort.class);
        reservationQueryPort = mock(ReservationQueryPort.class);
        businessAccessPort = mock(BusinessAccessPort.class);
        serviceAt(NOW);
        when(businessLookupPort.findActiveById(BUSINESS_ID))
                .thenReturn(Optional.of(activeBusiness()));
    }

    @Test
    void holdRejectsExpiredSlotIdDate() {
        givenBookingContext(ResourceBookingOverrides.none());
        final var expiredSlotId =
                SlotId.of(BUSINESS_ID, RESOURCE_ID, NOW.minusSeconds(60), NOW.minusSeconds(30))
                        .value();

        assertThrows(
                SlotUnavailableException.class,
                () ->
                        service.hold(
                                new HoldReservationCommand(
                                        BUSINESS_ID,
                                        RESOURCE_ID,
                                        CUSTOMER_ACCOUNT_ID,
                                        expiredSlotId)));

        verify(slotLockPort, never()).lockSlot(RESOURCE_ID, NOW.minusSeconds(60));
    }

    @Test
    void holdRejectsSlotOutsideGeneratedSlots() {
        givenBookingContext(ResourceBookingOverrides.none());
        when(scheduleQueryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, SLOT_DATE))
                .thenReturn(Optional.empty());
        when(scheduleQueryPort.findWeekly(BUSINESS_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        assertThrows(SlotUnavailableException.class, () -> service.hold(validHoldCommand()));

        verify(slotLockPort, never()).lockSlot(RESOURCE_ID, SLOT_START_AT);
    }

    @Test
    void holdLocksSlotBeforeActiveBlockerQuery() {
        givenHoldableSlot(ResourceBookingOverrides.none());
        when(reservationQueryPort.findActiveBlockers(
                        BUSINESS_ID, RESOURCE_ID, SLOT_START_AT, SLOT_END_AT, NOW))
                .thenReturn(List.of());

        final var result = service.hold(validHoldCommand());

        assertEquals(ReservationState.HELD, result.state());
        assertEquals(NOW.plusSeconds(600), result.holdExpiresAt());
        final var inOrder = inOrder(slotLockPort, reservationQueryPort);
        inOrder.verify(slotLockPort).lockSlot(RESOURCE_ID, SLOT_START_AT);
        inOrder.verify(reservationQueryPort)
                .findActiveBlockers(BUSINESS_ID, RESOURCE_ID, SLOT_START_AT, SLOT_END_AT, NOW);

        final var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationCommandPort).save(captor.capture());
        assertEquals(CUSTOMER_ACCOUNT_ID, captor.getValue().customerAccountId());
    }

    @Test
    void confirmRejectsExpiredHold() {
        final var reservation =
                Reservation.hold(
                        BUSINESS_ID,
                        RESOURCE_ID,
                        CUSTOMER_ACCOUNT_ID,
                        SLOT_START_AT,
                        SLOT_END_AT,
                        NOW.minusSeconds(1),
                        NOW.minusSeconds(60));
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, reservation.id()))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationHoldExpiredException.class,
                () ->
                        service.confirm(
                                new ConfirmReservationCommand(
                                        BUSINESS_ID, reservation.id(), CUSTOMER_ACCOUNT_ID)));
    }

    @Test
    void releaseRejectsConfirmedReservation() {
        final var reservation =
                Reservation.hold(
                                BUSINESS_ID,
                                RESOURCE_ID,
                                CUSTOMER_ACCOUNT_ID,
                                SLOT_START_AT,
                                SLOT_END_AT,
                                NOW.plusSeconds(60),
                                NOW.minusSeconds(60))
                        .confirm(NOW);
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, reservation.id()))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        service.release(
                                new ReleaseReservationCommand(
                                        BUSINESS_ID, reservation.id(), CUSTOMER_ACCOUNT_ID)));
    }

    @Test
    void customerCancelUsesCancellationWindow() {
        serviceAt(Instant.parse("2026-05-24T23:31:00Z"));
        givenBookingContext(ResourceBookingOverrides.none());
        final var reservation =
                Reservation.hold(
                                BUSINESS_ID,
                                RESOURCE_ID,
                                CUSTOMER_ACCOUNT_ID,
                                SLOT_START_AT,
                                SLOT_END_AT,
                                NOW.plusSeconds(60),
                                NOW.minusSeconds(60))
                        .confirm(NOW);
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, reservation.id()))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        service.cancel(
                                new CancelReservationCommand(
                                        BUSINESS_ID,
                                        reservation.id(),
                                        CUSTOMER_ACCOUNT_ID,
                                        ReservationCancellationActor.CUSTOMER)));
    }

    @Test
    void businessCancelCanCancelHeldOrConfirmed() {
        final var held =
                Reservation.hold(
                        BUSINESS_ID,
                        RESOURCE_ID,
                        CUSTOMER_ACCOUNT_ID,
                        SLOT_START_AT,
                        SLOT_END_AT,
                        NOW.plusSeconds(60),
                        NOW.minusSeconds(60));
        final var confirmed =
                Reservation.hold(
                                BUSINESS_ID,
                                RESOURCE_ID,
                                CUSTOMER_ACCOUNT_ID,
                                SLOT_START_AT.plusSeconds(3600),
                                SLOT_END_AT.plusSeconds(3600),
                                NOW.plusSeconds(60),
                                NOW.minusSeconds(60))
                        .confirm(NOW);
        when(businessAccessPort.hasBusinessAccess(STAFF_ACCOUNT_ID, BUSINESS_ID)).thenReturn(true);
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, held.id()))
                .thenReturn(Optional.of(held));
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, confirmed.id()))
                .thenReturn(Optional.of(confirmed));

        final var heldResult =
                service.cancel(
                        new CancelReservationCommand(
                                BUSINESS_ID,
                                held.id(),
                                STAFF_ACCOUNT_ID,
                                ReservationCancellationActor.BUSINESS));
        final var confirmedResult =
                service.cancel(
                        new CancelReservationCommand(
                                BUSINESS_ID,
                                confirmed.id(),
                                STAFF_ACCOUNT_ID,
                                ReservationCancellationActor.BUSINESS));

        assertEquals(ReservationState.BUSINESS_CANCELLED, heldResult.state());
        assertEquals(ReservationState.BUSINESS_CANCELLED, confirmedResult.state());
        assertEquals(TIMEZONE.value(), heldResult.businessZone());
        assertEquals(TIMEZONE.value(), confirmedResult.businessZone());
    }

    @Test
    void checkInRequiresConfirmedAndStartReached() {
        final var reservation = confirmedReservation();
        when(businessAccessPort.hasBusinessAccess(STAFF_ACCOUNT_ID, BUSINESS_ID)).thenReturn(true);
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, reservation.id()))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        service.checkIn(
                                new CheckInReservationCommand(
                                        BUSINESS_ID, reservation.id(), STAFF_ACCOUNT_ID)));

        serviceAt(SLOT_START_AT);
        final var result =
                service.checkIn(
                        new CheckInReservationCommand(
                                BUSINESS_ID, reservation.id(), STAFF_ACCOUNT_ID));

        assertEquals(ReservationState.CHECKED_IN, result.state());
        assertEquals(TIMEZONE.value(), result.businessZone());
    }

    @Test
    void noShowRequiresConfirmedAndEndReached() {
        final var reservation = confirmedReservation();
        when(businessAccessPort.hasBusinessAccess(STAFF_ACCOUNT_ID, BUSINESS_ID)).thenReturn(true);
        when(reservationCommandPort.findByBusinessIdAndIdForUpdate(BUSINESS_ID, reservation.id()))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        service.markNoShow(
                                new MarkNoShowReservationCommand(
                                        BUSINESS_ID, reservation.id(), STAFF_ACCOUNT_ID)));

        serviceAt(SLOT_END_AT);
        final var result =
                service.markNoShow(
                        new MarkNoShowReservationCommand(
                                BUSINESS_ID, reservation.id(), STAFF_ACCOUNT_ID));

        assertEquals(ReservationState.NO_SHOW, result.state());
        assertEquals(TIMEZONE.value(), result.businessZone());
    }

    @Test
    void listBusinessReservationsRequiresBusinessAccess() {
        when(businessAccessPort.hasBusinessAccess(STAFF_ACCOUNT_ID, BUSINESS_ID)).thenReturn(false);

        assertThrows(
                ReservationAccessDeniedException.class,
                () ->
                        service.listBusinessReservations(
                                new ListBusinessReservationsQuery(
                                        BUSINESS_ID,
                                        STAFF_ACCOUNT_ID,
                                        SLOT_DATE,
                                        null,
                                        null,
                                        null)));

        verify(reservationQueryPort, never())
                .findByBusinessDateWindow(any(), any(), any(), any(), any());
    }

    @Test
    void listBusinessReservationsUsesBusinessTimezoneAndFiltersByDerivedState() {
        when(businessAccessPort.hasBusinessAccess(STAFF_ACCOUNT_ID, BUSINESS_ID)).thenReturn(true);
        final var expiredHold =
                Reservation.hold(
                        BUSINESS_ID,
                        RESOURCE_ID,
                        CUSTOMER_ACCOUNT_ID,
                        SLOT_START_AT.minusSeconds(3600),
                        SLOT_END_AT.minusSeconds(3600),
                        NOW.minusSeconds(1),
                        NOW.minusSeconds(7200));
        final var confirmed = confirmedReservation();
        when(reservationQueryPort.findByBusinessDateWindow(
                        BUSINESS_ID,
                        Instant.parse("2026-05-24T15:00:00Z"),
                        Instant.parse("2026-05-25T15:00:00Z"),
                        RESOURCE_ID,
                        CUSTOMER_ACCOUNT_ID))
                .thenReturn(List.of(expiredHold, confirmed));

        final var results =
                service.listBusinessReservations(
                        new ListBusinessReservationsQuery(
                                BUSINESS_ID,
                                STAFF_ACCOUNT_ID,
                                SLOT_DATE,
                                RESOURCE_ID,
                                CUSTOMER_ACCOUNT_ID,
                                ReservationState.CONFIRMED));

        assertEquals(1, results.size());
        assertEquals(ReservationState.CONFIRMED, results.getFirst().state());
        assertEquals(TIMEZONE.value(), results.getFirst().businessZone());
    }

    @Test
    void listBusinessReservationsQueryRejectsNullRequiredFields() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new ListBusinessReservationsQuery(
                                null, STAFF_ACCOUNT_ID, SLOT_DATE, null, null, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new ListBusinessReservationsQuery(
                                BUSINESS_ID, null, SLOT_DATE, null, null, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new ListBusinessReservationsQuery(
                                BUSINESS_ID, STAFF_ACCOUNT_ID, null, null, null, null));
    }

    private void serviceAt(final Instant now) {
        service =
                new ReservationService(
                        businessLookupPort,
                        settingsQueryPort,
                        resourceQueryPort,
                        scheduleQueryPort,
                        slotLockPort,
                        reservationCommandPort,
                        reservationQueryPort,
                        businessAccessPort,
                        Clock.fixed(now, ZoneOffset.UTC));
    }

    private void givenHoldableSlot(final ResourceBookingOverrides overrides) {
        givenBookingContext(overrides);
        when(scheduleQueryPort.findDateOverride(BUSINESS_ID, RESOURCE_ID, SLOT_DATE))
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
    }

    private void givenBookingContext(final ResourceBookingOverrides overrides) {
        when(settingsQueryPort.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(settings()));
        when(resourceQueryPort.findByBusinessIdAndId(BUSINESS_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource(overrides)));
    }

    private static BusinessLookupPort.BusinessView activeBusiness() {
        return new BusinessLookupPort.BusinessView(BUSINESS_ID, "Salon A", "salon-a", TIMEZONE);
    }

    private static HoldReservationCommand validHoldCommand() {
        return new HoldReservationCommand(
                BUSINESS_ID,
                RESOURCE_ID,
                CUSTOMER_ACCOUNT_ID,
                SlotId.of(BUSINESS_ID, RESOURCE_ID, SLOT_START_AT, SLOT_END_AT).value());
    }

    private static Reservation confirmedReservation() {
        return Reservation.hold(
                        BUSINESS_ID,
                        RESOURCE_ID,
                        CUSTOMER_ACCOUNT_ID,
                        SLOT_START_AT,
                        SLOT_END_AT,
                        NOW.plusSeconds(60),
                        NOW.minusSeconds(60))
                .confirm(NOW);
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

    private static Resource resource(final ResourceBookingOverrides overrides) {
        return Resource.reconstitute(
                RESOURCE_ID,
                BUSINESS_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                null,
                ResourceStatus.ACTIVE,
                overrides,
                NOW,
                NOW);
    }
}

package io.resrv.application.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.application.availability.out.DateAvailabilityOverrideQueryPort;
import io.resrv.application.availability.out.WeeklyAvailabilityQueryPort;
import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.reservation.in.CancelCustomerReservationCommand;
import io.resrv.application.reservation.in.ConfirmReservationCommand;
import io.resrv.application.reservation.in.HoldReservationCommand;
import io.resrv.application.reservation.in.ListAvailableSlotsQuery;
import io.resrv.application.reservation.in.ListCustomerReservationsQuery;
import io.resrv.application.reservation.in.ListResourceReservationsQuery;
import io.resrv.application.reservation.out.ReservationCommandPort;
import io.resrv.application.reservation.out.ReservationQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.application.security.ForbiddenOperationException;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.customer.CustomerName;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.reservation.SlotUnavailableException;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.TenantStatus;
import io.resrv.domain.tenant.Timezone;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReservationServiceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final CustomerId CUSTOMER_ID = CustomerId.create();
    private static final CustomerId OTHER_CUSTOMER_ID = CustomerId.create();
    private static final ReservationId RESERVATION_ID = ReservationId.create();
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final LocalDate SLOT_DATE = LocalDate.of(2025, 1, 6);
    private static final Instant SLOT_09 = Instant.parse("2025-01-06T09:00:00Z");
    private static final Instant SLOT_10 = Instant.parse("2025-01-06T10:00:00Z");
    private static final Instant SLOT_11 = Instant.parse("2025-01-06T11:00:00Z");
    private static final Instant SLOT_12 = Instant.parse("2025-01-06T12:00:00Z");

    private TenantQueryPort tenantQueryPort;
    private CustomerQueryPort customerQueryPort;
    private ResourceQueryPort resourceQueryPort;
    private WeeklyAvailabilityQueryPort weeklyAvailabilityQueryPort;
    private DateAvailabilityOverrideQueryPort dateAvailabilityOverrideQueryPort;
    private ReservationCommandPort reservationCommandPort;
    private ReservationQueryPort reservationQueryPort;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        tenantQueryPort = mock(TenantQueryPort.class);
        customerQueryPort = mock(CustomerQueryPort.class);
        resourceQueryPort = mock(ResourceQueryPort.class);
        weeklyAvailabilityQueryPort = mock(WeeklyAvailabilityQueryPort.class);
        dateAvailabilityOverrideQueryPort = mock(DateAvailabilityOverrideQueryPort.class);
        reservationCommandPort = mock(ReservationCommandPort.class);
        reservationQueryPort = mock(ReservationQueryPort.class);
        service =
                new ReservationService(
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        tenantQueryPort,
                        customerQueryPort,
                        resourceQueryPort,
                        weeklyAvailabilityQueryPort,
                        dateAvailabilityOverrideQueryPort,
                        reservationCommandPort,
                        reservationQueryPort);
    }

    @Test
    void listAvailableSlots_returnsWeeklySlotsMinusActiveOverlaps() {
        stubTenant();
        stubActiveResource();
        stubWeeklyNineToNoon();
        when(reservationQueryPort.existsActiveOverlap(TENANT_ID, RESOURCE_ID, SLOT_10, SLOT_11))
                .thenReturn(true);

        final var slots =
                service.listAvailableSlots(
                        new ListAvailableSlotsQuery(TENANT_ID, RESOURCE_ID, SLOT_DATE));

        assertEquals(2, slots.size());
        assertEquals(SLOT_09, slots.getFirst().startAt());
        assertEquals(SLOT_11, slots.getLast().startAt());
        verify(reservationCommandPort).expireHoldsDueAtOrBefore(FIXED_NOW);
    }

    @Test
    void listAvailableSlots_closedDateOverrideSuppressesWeeklySchedule() {
        stubTenant();
        stubActiveResource();
        when(dateAvailabilityOverrideQueryPort.findByTenantIdAndResourceIdAndDate(
                        TENANT_ID, RESOURCE_ID, SLOT_DATE))
                .thenReturn(
                        Optional.of(
                                DateAvailabilityOverride.closed(
                                        TENANT_ID, RESOURCE_ID, SLOT_DATE, CREATED_AT)));

        final var slots =
                service.listAvailableSlots(
                        new ListAvailableSlotsQuery(TENANT_ID, RESOURCE_ID, SLOT_DATE));

        assertTrue(slots.isEmpty());
        verify(weeklyAvailabilityQueryPort, never())
                .findByTenantIdAndResourceIdAndDayOfWeek(any(), any(), any());
    }

    @Test
    void hold_savesHeldReservationWhenRequestedSlotIsAvailable() {
        stubTenant();
        stubActiveCustomer();
        stubActiveResource();
        stubWeeklyNineToNoon();

        final var result =
                service.hold(
                        new HoldReservationCommand(TENANT_ID, CUSTOMER_ID, RESOURCE_ID, SLOT_09));

        assertEquals(ReservationStatus.HELD, result.status());
        assertEquals(SLOT_09, result.startAt());
        assertEquals(SLOT_10, result.endAt());
        assertEquals(FIXED_NOW.plusSeconds(15 * 60L), result.holdExpiresAt());

        final var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationCommandPort).save(captor.capture());
        assertEquals(CUSTOMER_ID, captor.getValue().customerId());
        assertEquals(RESOURCE_ID, captor.getValue().resourceId());
    }

    @Test
    void hold_unavailableRequestedSlot_throwsAndDoesNotSave() {
        stubTenant();
        stubActiveCustomer();
        stubActiveResource();
        stubWeeklyNineToNoon();
        when(reservationQueryPort.existsActiveOverlap(TENANT_ID, RESOURCE_ID, SLOT_09, SLOT_10))
                .thenReturn(true);

        assertThrows(
                SlotUnavailableException.class,
                () ->
                        service.hold(
                                new HoldReservationCommand(
                                        TENANT_ID, CUSTOMER_ID, RESOURCE_ID, SLOT_09)));

        verify(reservationCommandPort, never()).save(any());
    }

    @Test
    void confirm_updatesHeldReservationOwnedByCustomer() {
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(heldReservation(CUSTOMER_ID, SLOT_09)));

        final var result =
                service.confirm(
                        new ConfirmReservationCommand(TENANT_ID, CUSTOMER_ID, RESERVATION_ID));

        assertEquals(ReservationStatus.CONFIRMED, result.status());
        assertEquals(FIXED_NOW, result.confirmedAt());
        verify(reservationCommandPort).save(any(Reservation.class));
    }

    @Test
    void confirm_otherCustomer_throwsForbiddenAndDoesNotSave() {
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(heldReservation(CUSTOMER_ID, SLOT_09)));

        assertThrows(
                ForbiddenOperationException.class,
                () ->
                        service.confirm(
                                new ConfirmReservationCommand(
                                        TENANT_ID, OTHER_CUSTOMER_ID, RESERVATION_ID)));

        verify(reservationCommandPort, never()).save(any());
    }

    @Test
    void listCustomerReservations_validatesCustomerAndSortsNewestFirst() {
        stubActiveCustomer();
        final var older = confirmedReservation(CUSTOMER_ID, SLOT_09);
        final var newer = confirmedReservation(CUSTOMER_ID, SLOT_11);
        when(reservationQueryPort.findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(List.of(older, newer));

        final var results =
                service.listCustomerReservations(
                        new ListCustomerReservationsQuery(TENANT_ID, CUSTOMER_ID));

        assertEquals(
                List.of(SLOT_11, SLOT_09),
                results.stream().map(result -> result.startAt()).toList());
        verify(reservationCommandPort).expireHoldsDueAtOrBefore(FIXED_NOW);
    }

    @Test
    void listCustomerReservations_inactiveCustomer_throwsForbidden() {
        when(customerQueryPort.findByTenantIdAndId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer(false)));

        assertThrows(
                ForbiddenOperationException.class,
                () ->
                        service.listCustomerReservations(
                                new ListCustomerReservationsQuery(TENANT_ID, CUSTOMER_ID)));

        verify(reservationQueryPort, never()).findByTenantIdAndCustomerId(any(), any());
    }

    @Test
    void cancel_customerOwnedReservationWithinWindow_marksCancelled() {
        stubTenant();
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(CUSTOMER_ID, SLOT_09)));

        final var result =
                service.cancel(
                        new CancelCustomerReservationCommand(
                                TENANT_ID, CUSTOMER_ID, RESERVATION_ID));

        assertEquals(ReservationStatus.CUSTOMER_CANCELLED, result.status());
        assertEquals(FIXED_NOW, result.cancelledAt());
        verify(reservationCommandPort).save(any(Reservation.class));
    }

    @Test
    void listResourceReservations_usesTenantDayWindowAndSortsAscending() {
        stubTenant();
        stubActiveResource();
        final var later = confirmedReservation(CUSTOMER_ID, SLOT_11);
        final var earlier = confirmedReservation(CUSTOMER_ID, SLOT_09);
        when(reservationQueryPort.findByTenantIdAndResourceIdBetween(
                        TENANT_ID,
                        RESOURCE_ID,
                        SLOT_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        SLOT_DATE.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(List.of(later, earlier));

        final var results =
                service.listResourceReservations(
                        new ListResourceReservationsQuery(TENANT_ID, RESOURCE_ID, SLOT_DATE));

        assertEquals(
                List.of(SLOT_09, SLOT_11),
                results.stream().map(result -> result.startAt()).toList());
    }

    private void stubTenant() {
        when(tenantQueryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant()));
    }

    private void stubActiveCustomer() {
        when(customerQueryPort.findByTenantIdAndId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer(true)));
    }

    private void stubActiveResource() {
        when(resourceQueryPort.findByTenantIdAndId(TENANT_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource(ResourceStatus.ACTIVE)));
    }

    private void stubWeeklyNineToNoon() {
        when(dateAvailabilityOverrideQueryPort.findByTenantIdAndResourceIdAndDate(
                        TENANT_ID, RESOURCE_ID, SLOT_DATE))
                .thenReturn(Optional.empty());
        when(weeklyAvailabilityQueryPort.findByTenantIdAndResourceIdAndDayOfWeek(
                        TENANT_ID, RESOURCE_ID, DayOfWeek.MONDAY))
                .thenReturn(
                        Optional.of(
                                WeeklyAvailability.create(
                                        TENANT_ID,
                                        RESOURCE_ID,
                                        DayOfWeek.MONDAY,
                                        LocalTime.of(9, 0),
                                        LocalTime.of(12, 0),
                                        CREATED_AT)));
    }

    private static Tenant tenant() {
        return Tenant.reconstitute(
                TENANT_ID,
                new TenantName("My Salon"),
                new Slug("my-salon"),
                new Timezone(ZoneId.of("UTC")),
                new SlotDuration(60),
                new HoldTtl(15),
                new CancellationWindow(60),
                TenantStatus.ACTIVE,
                CREATED_AT);
    }

    private static Customer customer(final boolean active) {
        return Customer.reconstitute(
                CUSTOMER_ID,
                TENANT_ID,
                new CustomerEmail("customer@example.com"),
                new CustomerName("Jane Customer"),
                "hashed-password",
                active,
                CREATED_AT);
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

    private static Reservation heldReservation(final CustomerId customerId, final Instant startAt) {
        return Reservation.reconstitute(
                RESERVATION_ID,
                TENANT_ID,
                RESOURCE_ID,
                customerId,
                startAt,
                startAt.plusSeconds(60 * 60L),
                ReservationStatus.HELD,
                FIXED_NOW.plusSeconds(15 * 60L),
                CREATED_AT,
                CREATED_AT,
                null,
                null);
    }

    private static Reservation confirmedReservation(
            final CustomerId customerId, final Instant startAt) {
        return Reservation.reconstitute(
                ReservationId.create(),
                TENANT_ID,
                RESOURCE_ID,
                customerId,
                startAt,
                startAt.plusSeconds(60 * 60L),
                ReservationStatus.CONFIRMED,
                null,
                CREATED_AT,
                CREATED_AT,
                CREATED_AT,
                null);
    }
}

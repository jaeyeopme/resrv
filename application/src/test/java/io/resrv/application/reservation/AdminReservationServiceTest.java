package io.resrv.application.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.reservation.in.AdminCancelReservationCommand;
import io.resrv.application.reservation.in.CheckInReservationCommand;
import io.resrv.application.reservation.in.ListAdminReservationsQuery;
import io.resrv.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.application.reservation.out.ReservationCommandPort;
import io.resrv.application.reservation.out.ReservationQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.customer.CustomerName;
import io.resrv.domain.customer.CustomerNotFoundException;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationInvalidStateException;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceNotFoundException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminReservationServiceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final CustomerId CUSTOMER_ID = CustomerId.create();
    private static final ReservationId RESERVATION_ID = ReservationId.create();
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant FIXED_NOW = Instant.parse("2025-01-06T10:30:00Z");
    private static final LocalDate SLOT_DATE = LocalDate.of(2025, 1, 6);
    private static final Instant SLOT_09 = Instant.parse("2025-01-06T09:00:00Z");
    private static final Instant SLOT_10 = Instant.parse("2025-01-06T10:00:00Z");
    private static final Instant SLOT_11 = Instant.parse("2025-01-06T11:00:00Z");

    private TenantQueryPort tenantQueryPort;
    private ResourceQueryPort resourceQueryPort;
    private CustomerQueryPort customerQueryPort;
    private ReservationCommandPort reservationCommandPort;
    private ReservationQueryPort reservationQueryPort;
    private AdminReservationService service;

    @BeforeEach
    void setUp() {
        tenantQueryPort = mock(TenantQueryPort.class);
        resourceQueryPort = mock(ResourceQueryPort.class);
        customerQueryPort = mock(CustomerQueryPort.class);
        reservationCommandPort = mock(ReservationCommandPort.class);
        reservationQueryPort = mock(ReservationQueryPort.class);
        service =
                new AdminReservationService(
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        tenantQueryPort,
                        resourceQueryPort,
                        customerQueryPort,
                        reservationCommandPort,
                        reservationQueryPort);
    }

    @Test
    void listAdminReservations_validatesFiltersAndSortsByStartThenId() {
        stubTenant();
        stubResource();
        stubCustomer();
        final var later = confirmedReservation(ReservationId.create(), SLOT_11);
        final var earlier = confirmedReservation(ReservationId.create(), SLOT_09);
        when(reservationQueryPort.findByTenantIdBetweenWithFilters(
                        TENANT_ID,
                        SLOT_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        SLOT_DATE.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                        Optional.of(RESOURCE_ID),
                        Optional.of(CUSTOMER_ID),
                        Optional.of(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of(later, earlier));

        final var results =
                service.listAdminReservations(
                        new ListAdminReservationsQuery(
                                TENANT_ID,
                                SLOT_DATE,
                                Optional.of(RESOURCE_ID),
                                Optional.of(CUSTOMER_ID),
                                Optional.of(ReservationStatus.CONFIRMED)));

        assertEquals(List.of(SLOT_09, SLOT_11), results.stream().map(r -> r.startAt()).toList());
        verify(reservationCommandPort).expireHoldsDueAtOrBefore(FIXED_NOW);
    }

    @Test
    void listAdminReservations_missingResourceOrCustomerThrowsNotFound() {
        stubTenant();
        when(resourceQueryPort.findByTenantIdAndId(TENANT_ID, RESOURCE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.listAdminReservations(
                                new ListAdminReservationsQuery(
                                        TENANT_ID,
                                        SLOT_DATE,
                                        Optional.of(RESOURCE_ID),
                                        Optional.empty(),
                                        Optional.empty())));

        stubResource();
        when(customerQueryPort.findByTenantIdAndId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());
        assertThrows(
                CustomerNotFoundException.class,
                () ->
                        service.listAdminReservations(
                                new ListAdminReservationsQuery(
                                        TENANT_ID,
                                        SLOT_DATE,
                                        Optional.of(RESOURCE_ID),
                                        Optional.of(CUSTOMER_ID),
                                        Optional.empty())));
    }

    @Test
    void adminCancelSavesAdminCancelledReservation() {
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(RESERVATION_ID, SLOT_09)));

        final var result =
                service.adminCancel(new AdminCancelReservationCommand(TENANT_ID, RESERVATION_ID));

        assertEquals(ReservationStatus.ADMIN_CANCELLED, result.status());
        assertEquals(FIXED_NOW, result.cancelledAt());
        verifySavedStatus(ReservationStatus.ADMIN_CANCELLED, FIXED_NOW);
    }

    @Test
    void checkInRequiresConfirmedReservationAtOrAfterStart() {
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(RESERVATION_ID, SLOT_09)));

        final var result =
                service.checkIn(new CheckInReservationCommand(TENANT_ID, RESERVATION_ID));

        assertEquals(ReservationStatus.CHECKED_IN, result.status());
        verifySavedStatus(ReservationStatus.CHECKED_IN, FIXED_NOW);
    }

    @Test
    void checkInBeforeStartThrowsConflictAndDoesNotSave() {
        final var earlyService = serviceAt(SLOT_09.minusSeconds(1));
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(RESERVATION_ID, SLOT_09)));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        earlyService.checkIn(
                                new CheckInReservationCommand(TENANT_ID, RESERVATION_ID)));
        verify(reservationCommandPort, never()).save(any());
    }

    @Test
    void markNoShowRequiresConfirmedReservationAtOrAfterEnd() {
        final var noShowService = serviceAt(SLOT_10);
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(RESERVATION_ID, SLOT_09)));

        final var result =
                noShowService.markNoShow(
                        new MarkNoShowReservationCommand(TENANT_ID, RESERVATION_ID));

        assertEquals(ReservationStatus.NO_SHOW, result.status());
        verifySavedStatus(ReservationStatus.NO_SHOW, SLOT_10);
    }

    @Test
    void markNoShowBeforeEndThrowsConflictAndDoesNotSave() {
        final var earlyService = serviceAt(SLOT_10.minusSeconds(1));
        when(reservationQueryPort.findByTenantIdAndId(TENANT_ID, RESERVATION_ID))
                .thenReturn(Optional.of(confirmedReservation(RESERVATION_ID, SLOT_09)));

        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        earlyService.markNoShow(
                                new MarkNoShowReservationCommand(TENANT_ID, RESERVATION_ID)));
        verify(reservationCommandPort, never()).save(any());
    }

    private AdminReservationService serviceAt(final Instant now) {
        return new AdminReservationService(
                Clock.fixed(now, ZoneOffset.UTC),
                tenantQueryPort,
                resourceQueryPort,
                customerQueryPort,
                reservationCommandPort,
                reservationQueryPort);
    }

    private void verifySavedStatus(final ReservationStatus status, final Instant now) {
        final var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationCommandPort).save(captor.capture());
        assertEquals(status, captor.getValue().status());
        verify(reservationCommandPort).expireHoldsDueAtOrBefore(now);
    }

    private void stubTenant() {
        when(tenantQueryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant()));
    }

    private void stubResource() {
        when(resourceQueryPort.findByTenantIdAndId(TENANT_ID, RESOURCE_ID))
                .thenReturn(Optional.of(resource()));
    }

    private void stubCustomer() {
        when(customerQueryPort.findByTenantIdAndId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer()));
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

    private static Resource resource() {
        return Resource.reconstitute(
                RESOURCE_ID,
                TENANT_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                ResourceDescription.empty(),
                ResourceStatus.ACTIVE,
                CREATED_AT,
                CREATED_AT);
    }

    private static Customer customer() {
        return Customer.reconstitute(
                CUSTOMER_ID,
                TENANT_ID,
                new CustomerEmail("customer@example.com"),
                new CustomerName("Jane Customer"),
                "hashed-password",
                true,
                CREATED_AT);
    }

    private static Reservation confirmedReservation(
            final ReservationId reservationId, final Instant startAt) {
        return Reservation.reconstitute(
                reservationId,
                TENANT_ID,
                RESOURCE_ID,
                CUSTOMER_ID,
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

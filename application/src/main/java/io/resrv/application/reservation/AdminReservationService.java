package io.resrv.application.reservation;

import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.reservation.in.AdminCancelReservationCommand;
import io.resrv.application.reservation.in.AdminCancelReservationUseCase;
import io.resrv.application.reservation.in.CheckInReservationCommand;
import io.resrv.application.reservation.in.CheckInReservationUseCase;
import io.resrv.application.reservation.in.ListAdminReservationsQuery;
import io.resrv.application.reservation.in.ListAdminReservationsUseCase;
import io.resrv.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.application.reservation.in.MarkNoShowReservationUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.application.reservation.out.ReservationCommandPort;
import io.resrv.application.reservation.out.ReservationQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.application.tenant.TenantNotFoundException;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.customer.CustomerNotFoundException;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationNotFoundException;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AdminReservationService
        implements ListAdminReservationsUseCase,
                AdminCancelReservationUseCase,
                CheckInReservationUseCase,
                MarkNoShowReservationUseCase {

    private final Clock clock;
    private final TenantQueryPort tenantQueryPort;
    private final ResourceQueryPort resourceQueryPort;
    private final CustomerQueryPort customerQueryPort;
    private final ReservationCommandPort reservationCommandPort;
    private final ReservationQueryPort reservationQueryPort;

    AdminReservationService(
            final Clock clock,
            final TenantQueryPort tenantQueryPort,
            final ResourceQueryPort resourceQueryPort,
            final CustomerQueryPort customerQueryPort,
            final ReservationCommandPort reservationCommandPort,
            final ReservationQueryPort reservationQueryPort) {
        this.clock = clock;
        this.tenantQueryPort = tenantQueryPort;
        this.resourceQueryPort = resourceQueryPort;
        this.customerQueryPort = customerQueryPort;
        this.reservationCommandPort = reservationCommandPort;
        this.reservationQueryPort = reservationQueryPort;
    }

    @Override
    public List<ReservationResult> listAdminReservations(final ListAdminReservationsQuery query) {
        expireDueHolds();
        final var tenant = findTenant(query.tenantId());
        query.resourceId()
                .ifPresent(resourceId -> ensureResourceExists(query.tenantId(), resourceId));
        query.customerId()
                .ifPresent(customerId -> ensureCustomerExists(query.tenantId(), customerId));
        final var window = dayWindow(tenant, query.date());
        return reservationQueryPort
                .findByTenantIdBetweenWithFilters(
                        query.tenantId(),
                        window.startAt(),
                        window.endAt(),
                        query.resourceId(),
                        query.customerId(),
                        query.status())
                .stream()
                .sorted(
                        Comparator.comparing(Reservation::startAt)
                                .thenComparing(reservation -> reservation.id().value()))
                .map(ReservationResult::from)
                .toList();
    }

    @Override
    public ReservationResult adminCancel(final AdminCancelReservationCommand command) {
        expireDueHolds();
        final var reservation = findReservation(command.tenantId(), command.reservationId());
        final var cancelled = reservation.cancelByAdmin(clock.instant());
        reservationCommandPort.save(cancelled);
        return ReservationResult.from(cancelled);
    }

    @Override
    public ReservationResult checkIn(final CheckInReservationCommand command) {
        expireDueHolds();
        final var reservation = findReservation(command.tenantId(), command.reservationId());
        final var checkedIn = reservation.checkIn(clock.instant());
        reservationCommandPort.save(checkedIn);
        return ReservationResult.from(checkedIn);
    }

    @Override
    public ReservationResult markNoShow(final MarkNoShowReservationCommand command) {
        expireDueHolds();
        final var reservation = findReservation(command.tenantId(), command.reservationId());
        final var noShow = reservation.markNoShow(clock.instant());
        reservationCommandPort.save(noShow);
        return ReservationResult.from(noShow);
    }

    private Tenant findTenant(final TenantId tenantId) {
        return tenantQueryPort
                .findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private void ensureResourceExists(final TenantId tenantId, final ResourceId resourceId) {
        resourceQueryPort
                .findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(tenantId, resourceId));
    }

    private void ensureCustomerExists(final TenantId tenantId, final CustomerId customerId) {
        customerQueryPort
                .findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private Reservation findReservation(
            final TenantId tenantId, final ReservationId reservationId) {
        return reservationQueryPort
                .findByTenantIdAndId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(tenantId, reservationId));
    }

    private void expireDueHolds() {
        reservationCommandPort.expireHoldsDueAtOrBefore(clock.instant());
    }

    private static DayWindow dayWindow(final Tenant tenant, final LocalDate date) {
        final var start = date.atStartOfDay(tenant.timezone().value()).toInstant();
        final var end = date.plusDays(1).atStartOfDay(tenant.timezone().value()).toInstant();
        return new DayWindow(start, end);
    }

    private record DayWindow(Instant startAt, Instant endAt) {}
}

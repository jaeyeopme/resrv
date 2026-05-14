package io.resrv.application.reservation;

import io.resrv.application.availability.out.DateAvailabilityOverrideQueryPort;
import io.resrv.application.availability.out.WeeklyAvailabilityQueryPort;
import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.reservation.in.CancelCustomerReservationCommand;
import io.resrv.application.reservation.in.CancelCustomerReservationUseCase;
import io.resrv.application.reservation.in.ConfirmReservationCommand;
import io.resrv.application.reservation.in.ConfirmReservationUseCase;
import io.resrv.application.reservation.in.HoldReservationCommand;
import io.resrv.application.reservation.in.HoldReservationUseCase;
import io.resrv.application.reservation.in.ListAvailableSlotsQuery;
import io.resrv.application.reservation.in.ListAvailableSlotsUseCase;
import io.resrv.application.reservation.in.ListCustomerReservationsQuery;
import io.resrv.application.reservation.in.ListCustomerReservationsUseCase;
import io.resrv.application.reservation.in.ListResourceReservationsQuery;
import io.resrv.application.reservation.in.ListResourceReservationsUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.application.reservation.in.SlotResult;
import io.resrv.application.reservation.out.ReservationCommandPort;
import io.resrv.application.reservation.out.ReservationQueryPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.application.security.ForbiddenOperationException;
import io.resrv.application.tenant.TenantNotFoundException;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.customer.CustomerNotFoundException;
import io.resrv.domain.reservation.Reservation;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationNotFoundException;
import io.resrv.domain.reservation.SlotUnavailableException;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ReservationService
        implements ListAvailableSlotsUseCase,
                HoldReservationUseCase,
                ConfirmReservationUseCase,
                ListCustomerReservationsUseCase,
                CancelCustomerReservationUseCase,
                ListResourceReservationsUseCase {

    private final Clock clock;
    private final TenantQueryPort tenantQueryPort;
    private final CustomerQueryPort customerQueryPort;
    private final ResourceQueryPort resourceQueryPort;
    private final WeeklyAvailabilityQueryPort weeklyAvailabilityQueryPort;
    private final DateAvailabilityOverrideQueryPort dateAvailabilityOverrideQueryPort;
    private final ReservationCommandPort reservationCommandPort;
    private final ReservationQueryPort reservationQueryPort;

    ReservationService(
            final Clock clock,
            final TenantQueryPort tenantQueryPort,
            final CustomerQueryPort customerQueryPort,
            final ResourceQueryPort resourceQueryPort,
            final WeeklyAvailabilityQueryPort weeklyAvailabilityQueryPort,
            final DateAvailabilityOverrideQueryPort dateAvailabilityOverrideQueryPort,
            final ReservationCommandPort reservationCommandPort,
            final ReservationQueryPort reservationQueryPort) {
        this.clock = clock;
        this.tenantQueryPort = tenantQueryPort;
        this.customerQueryPort = customerQueryPort;
        this.resourceQueryPort = resourceQueryPort;
        this.weeklyAvailabilityQueryPort = weeklyAvailabilityQueryPort;
        this.dateAvailabilityOverrideQueryPort = dateAvailabilityOverrideQueryPort;
        this.reservationCommandPort = reservationCommandPort;
        this.reservationQueryPort = reservationQueryPort;
    }

    @Override
    public List<SlotResult> listAvailableSlots(final ListAvailableSlotsQuery query) {
        expireDueHolds();
        final var tenant = findTenant(query.tenantId());
        ensureActiveResource(query.tenantId(), query.resourceId());
        return availableSlots(tenant, query.resourceId(), query.date());
    }

    @Override
    public ReservationResult hold(final HoldReservationCommand command) {
        expireDueHolds();
        final var now = clock.instant();
        final var tenant = findTenant(command.tenantId());
        ensureActiveCustomer(command.tenantId(), command.customerId());
        ensureActiveResource(command.tenantId(), command.resourceId());

        final var endAt = command.startAt().plusSeconds(tenant.slotDuration().minutes() * 60L);
        final var requestedSlot = new SlotResult(command.startAt(), endAt);
        final var slotDate = LocalDate.ofInstant(command.startAt(), tenant.timezone().value());
        if (!availableSlots(tenant, command.resourceId(), slotDate).contains(requestedSlot)) {
            throw new SlotUnavailableException(command.resourceId(), command.startAt());
        }

        final var reservation =
                Reservation.hold(
                        command.tenantId(),
                        command.resourceId(),
                        command.customerId(),
                        command.startAt(),
                        endAt,
                        now.plusSeconds(tenant.holdTtl().minutes() * 60L),
                        now);
        reservationCommandPort.save(reservation);
        return ReservationResult.from(reservation);
    }

    @Override
    public ReservationResult confirm(final ConfirmReservationCommand command) {
        expireDueHolds();
        final var reservation = findReservation(command.tenantId(), command.reservationId());
        ensureCustomerOwnsReservation(command.customerId(), reservation);
        final var confirmed = reservation.confirm(clock.instant());
        reservationCommandPort.save(confirmed);
        return ReservationResult.from(confirmed);
    }

    @Override
    public List<ReservationResult> listCustomerReservations(
            final ListCustomerReservationsQuery query) {
        expireDueHolds();
        ensureActiveCustomer(query.tenantId(), query.customerId());
        return reservationQueryPort
                .findByTenantIdAndCustomerId(query.tenantId(), query.customerId())
                .stream()
                .sorted(Comparator.comparing(Reservation::startAt).reversed())
                .map(ReservationResult::from)
                .toList();
    }

    @Override
    public ReservationResult cancel(final CancelCustomerReservationCommand command) {
        expireDueHolds();
        final var tenant = findTenant(command.tenantId());
        final var reservation = findReservation(command.tenantId(), command.reservationId());
        ensureCustomerOwnsReservation(command.customerId(), reservation);
        final var cancellationCutoff =
                reservation.startAt().minusSeconds(tenant.cancellationWindow().minutes() * 60L);
        final var cancelled = reservation.cancelByCustomer(clock.instant(), cancellationCutoff);
        reservationCommandPort.save(cancelled);
        return ReservationResult.from(cancelled);
    }

    @Override
    public List<ReservationResult> listResourceReservations(
            final ListResourceReservationsQuery query) {
        expireDueHolds();
        final var tenant = findTenant(query.tenantId());
        ensureActiveResource(query.tenantId(), query.resourceId());
        final var window = dayWindow(tenant, query.date());
        return reservationQueryPort
                .findByTenantIdAndResourceIdBetween(
                        query.tenantId(), query.resourceId(), window.startAt(), window.endAt())
                .stream()
                .sorted(Comparator.comparing(Reservation::startAt))
                .map(ReservationResult::from)
                .toList();
    }

    private List<SlotResult> availableSlots(
            final Tenant tenant, final ResourceId resourceId, final LocalDate date) {
        final var window = availabilityWindow(tenant.id(), resourceId, date);
        if (window.isEmpty()) {
            return List.of();
        }
        final var startAt =
                date.atTime(window.get().startTime()).atZone(tenant.timezone().value()).toInstant();
        final var endAt =
                date.atTime(window.get().endTime()).atZone(tenant.timezone().value()).toInstant();
        final var slotSeconds = tenant.slotDuration().minutes() * 60L;

        final var candidates = new ArrayList<SlotResult>();
        for (var slotStart = startAt;
                !slotStart.plusSeconds(slotSeconds).isAfter(endAt);
                slotStart = slotStart.plusSeconds(slotSeconds)) {
            final var slotEnd = slotStart.plusSeconds(slotSeconds);
            if (!reservationQueryPort.existsActiveOverlap(
                    tenant.id(), resourceId, slotStart, slotEnd)) {
                candidates.add(new SlotResult(slotStart, slotEnd));
            }
        }
        return List.copyOf(candidates);
    }

    private Optional<LocalTimeWindow> availabilityWindow(
            final TenantId tenantId, final ResourceId resourceId, final LocalDate date) {
        final Optional<DateAvailabilityOverride> override =
                dateAvailabilityOverrideQueryPort.findByTenantIdAndResourceIdAndDate(
                        tenantId, resourceId, date);
        if (override.isPresent()) {
            final var value = override.get();
            if (value.closed()) {
                return Optional.empty();
            }
            return Optional.of(
                    new LocalTimeWindow(
                            Objects.requireNonNull(value.startTime()),
                            Objects.requireNonNull(value.endTime())));
        }
        return weeklyAvailabilityQueryPort
                .findByTenantIdAndResourceIdAndDayOfWeek(tenantId, resourceId, date.getDayOfWeek())
                .map(
                        availability ->
                                new LocalTimeWindow(
                                        availability.startTime(), availability.endTime()));
    }

    private static DayWindow dayWindow(final Tenant tenant, final LocalDate date) {
        final var start = date.atStartOfDay(tenant.timezone().value()).toInstant();
        final var end = date.plusDays(1).atStartOfDay(tenant.timezone().value()).toInstant();
        return new DayWindow(start, end);
    }

    private Tenant findTenant(final TenantId tenantId) {
        return tenantQueryPort
                .findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private Customer ensureActiveCustomer(final TenantId tenantId, final CustomerId customerId) {
        final var customer =
                customerQueryPort
                        .findByTenantIdAndId(tenantId, customerId)
                        .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (!customer.active()) {
            throw new ForbiddenOperationException("Customer is inactive");
        }
        return customer;
    }

    private Resource ensureActiveResource(final TenantId tenantId, final ResourceId resourceId) {
        final var resource =
                resourceQueryPort
                        .findByTenantIdAndId(tenantId, resourceId)
                        .orElseThrow(() -> new ResourceNotFoundException(tenantId, resourceId));
        if (resource.status() != ResourceStatus.ACTIVE) {
            throw new ResourceNotFoundException(tenantId, resourceId);
        }
        return resource;
    }

    private Reservation findReservation(
            final TenantId tenantId, final ReservationId reservationId) {
        return reservationQueryPort
                .findByTenantIdAndId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(tenantId, reservationId));
    }

    private static void ensureCustomerOwnsReservation(
            final CustomerId customerId, final Reservation reservation) {
        if (!reservation.customerId().equals(customerId)) {
            throw new ForbiddenOperationException("Reservation belongs to another customer");
        }
    }

    private void expireDueHolds() {
        reservationCommandPort.expireHoldsDueAtOrBefore(clock.instant());
    }

    private record LocalTimeWindow(LocalTime startTime, LocalTime endTime) {}

    private record DayWindow(Instant startAt, Instant endAt) {}
}

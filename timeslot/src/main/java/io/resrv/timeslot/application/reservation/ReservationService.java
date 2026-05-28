package io.resrv.timeslot.application.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.lock.out.SlotLockPort;
import io.resrv.timeslot.application.reservation.in.CancelReservationCommand;
import io.resrv.timeslot.application.reservation.in.CheckInReservationCommand;
import io.resrv.timeslot.application.reservation.in.ConfirmReservationCommand;
import io.resrv.timeslot.application.reservation.in.CustomerReservationDetailQuery;
import io.resrv.timeslot.application.reservation.in.CustomerReservationListQuery;
import io.resrv.timeslot.application.reservation.in.CustomerReservationPage;
import io.resrv.timeslot.application.reservation.in.CustomerReservationResult;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.in.ListBusinessReservationsQuery;
import io.resrv.timeslot.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReleaseReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReservationResult;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.resource.EffectiveBookingPolicy;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import io.resrv.timeslot.domain.slot.Slot;
import io.resrv.timeslot.domain.slot.SlotGenerator;
import io.resrv.timeslot.domain.slot.SlotId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private final BusinessLookupPort businessLookupPort;
    private final BusinessBookingSettingsQueryPort settingsQueryPort;
    private final ResourceQueryPort resourceQueryPort;
    private final ResourceScheduleQueryPort scheduleQueryPort;
    private final SlotLockPort slotLockPort;
    private final ReservationCommandPort reservationCommandPort;
    private final ReservationQueryPort reservationQueryPort;
    private final BusinessAccessPort businessAccessPort;
    private final Clock clock;

    public ReservationService(
            final BusinessLookupPort businessLookupPort,
            final BusinessBookingSettingsQueryPort settingsQueryPort,
            final ResourceQueryPort resourceQueryPort,
            final ResourceScheduleQueryPort scheduleQueryPort,
            final SlotLockPort slotLockPort,
            final ReservationCommandPort reservationCommandPort,
            final ReservationQueryPort reservationQueryPort,
            final BusinessAccessPort businessAccessPort,
            final Clock clock) {
        this.businessLookupPort = businessLookupPort;
        this.settingsQueryPort = settingsQueryPort;
        this.resourceQueryPort = resourceQueryPort;
        this.scheduleQueryPort = scheduleQueryPort;
        this.slotLockPort = slotLockPort;
        this.reservationCommandPort = reservationCommandPort;
        this.reservationQueryPort = reservationQueryPort;
        this.businessAccessPort = businessAccessPort;
        this.clock = clock;
    }

    public ReservationResult hold(final HoldReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var context = loadBookingContext(command.businessId(), command.resourceId());
        final var slot = decodeAndValidateSlot(command, context, now);

        slotLockPort.lockSlot(command.resourceId(), slot.startAt());
        if (!reservationQueryPort
                .findActiveBlockers(
                        command.businessId(),
                        command.resourceId(),
                        slot.startAt(),
                        slot.endAt(),
                        now)
                .isEmpty()) {
            throw new SlotUnavailableException(command.resourceId(), slot.startAt());
        }

        final var reservation =
                Reservation.hold(
                        command.businessId(),
                        command.resourceId(),
                        command.accountId(),
                        slot.startAt(),
                        slot.endAt(),
                        now.plusSeconds(context.policy().holdTtl().minutes() * 60L),
                        now);
        reservationCommandPort.save(reservation);
        return ReservationResult.from(reservation, now, context.business().timezone().value());
    }

    public ReservationResult confirm(final ConfirmReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireCustomerOwner(reservation, command.accountId());
        final var confirmed = reservation.confirm(now);
        reservationCommandPort.save(confirmed);
        return ReservationResult.from(confirmed, now, businessZone(command.businessId()));
    }

    public ReservationResult release(final ReleaseReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireCustomerOwner(reservation, command.accountId());
        final var released = reservation.release(now);
        reservationCommandPort.save(released);
        return ReservationResult.from(released, now, businessZone(command.businessId()));
    }

    public ReservationResult cancel(final CancelReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        final var cancelled =
                switch (command.actor()) {
                    case CUSTOMER -> cancelByCustomer(command, reservation, now);
                    case BUSINESS -> cancelByBusiness(command, reservation, now);
                };
        reservationCommandPort.save(cancelled);
        return ReservationResult.from(cancelled, now, businessZone(command.businessId()));
    }

    public ReservationResult checkIn(final CheckInReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireBusinessAccess(command.accountId(), command.businessId());
        final var checkedIn = reservation.checkIn(now);
        reservationCommandPort.save(checkedIn);
        return ReservationResult.from(checkedIn, now, businessZone(command.businessId()));
    }

    public ReservationResult markNoShow(final MarkNoShowReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireBusinessAccess(command.accountId(), command.businessId());
        final var noShow = reservation.markNoShow(now);
        reservationCommandPort.save(noShow);
        return ReservationResult.from(noShow, now, businessZone(command.businessId()));
    }

    public List<ReservationResult> listBusinessReservations(
            final ListBusinessReservationsQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        requireBusinessAccess(query.accountId(), query.businessId());
        final var business =
                businessLookupPort
                        .findActiveById(query.businessId())
                        .orElseThrow(() -> new BusinessNotAvailableException(query.businessId()));
        final var startInclusive =
                query.date().atStartOfDay(business.timezone().value()).toInstant();
        final var endExclusive =
                query.date().plusDays(1).atStartOfDay(business.timezone().value()).toInstant();
        final var now = clock.instant();
        return reservationQueryPort
                .findByBusinessDateWindow(
                        query.businessId(),
                        startInclusive,
                        endExclusive,
                        query.resourceId(),
                        query.customerAccountId())
                .stream()
                .map(
                        reservation ->
                                ReservationResult.from(
                                        reservation, now, business.timezone().value()))
                .filter(result -> query.state() == null || result.state() == query.state())
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerReservationPage listCustomerReservations(
            final CustomerReservationListQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var now = clock.instant();
        final var reservations =
                reservationQueryPort.findByCustomerAccountId(
                        query.accountId(),
                        query.page(),
                        query.size(),
                        query.state(),
                        query.upcoming(),
                        now);
        final var items =
                reservations.items().stream()
                        .map(reservation -> toCustomerReservationResult(reservation, now))
                        .toList();
        return new CustomerReservationPage(
                items,
                reservations.page(),
                reservations.size(),
                reservations.totalElements(),
                reservations.totalPages());
    }

    @Transactional(readOnly = true)
    public CustomerReservationResult getCustomerReservation(
            final CustomerReservationDetailQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var reservation =
                reservationQueryPort
                        .findById(query.reservationId())
                        .orElseThrow(
                                () ->
                                        customerReservationNotFound(
                                                query.reservationId(),
                                                CustomerReservationDenialFact
                                                        .RESERVATION_NOT_FOUND));
        if (!reservation.customerAccountId().equals(query.accountId())) {
            throw customerReservationNotFound(
                    query.reservationId(), CustomerReservationDenialFact.RESERVATION_NOT_OWNED);
        }
        return toCustomerReservationResult(reservation, clock.instant());
    }

    private Reservation cancelByCustomer(
            final CancelReservationCommand command,
            final Reservation reservation,
            final Instant now) {
        requireCustomerOwner(reservation, command.accountId());
        final var context = loadBookingContext(command.businessId(), reservation.resourceId());
        final var cutoff =
                reservation
                        .startAt()
                        .minus(Duration.ofMinutes(context.policy().cancellationWindow().minutes()));
        return reservation.cancelByCustomer(now, cutoff);
    }

    private Reservation cancelByBusiness(
            final CancelReservationCommand command,
            final Reservation reservation,
            final Instant now) {
        requireBusinessAccess(command.accountId(), command.businessId());
        return reservation.cancelByBusiness(now);
    }

    private Reservation findLocked(final BusinessId businessId, final ReservationId reservationId) {
        return reservationCommandPort
                .findByBusinessIdAndIdForUpdate(businessId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(businessId, reservationId));
    }

    private ZoneId businessZone(final BusinessId businessId) {
        return businessLookupPort
                .findActiveById(businessId)
                .orElseThrow(() -> new BusinessNotAvailableException(businessId))
                .timezone()
                .value();
    }

    private CustomerReservationResult toCustomerReservationResult(
            final Reservation reservation, final Instant now) {
        final var business =
                businessLookupPort
                        .findCurrentSummaryById(reservation.businessId())
                        .orElseThrow(
                                () -> new BusinessNotAvailableException(reservation.businessId()));
        final var resource =
                resourceQueryPort
                        .findByBusinessIdAndId(reservation.businessId(), reservation.resourceId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotAvailableException(
                                                reservation.businessId(),
                                                reservation.resourceId()));
        return new CustomerReservationResult(
                reservation.id().value(),
                new CustomerReservationResult.BusinessSummary(
                        business.id().value(),
                        business.name(),
                        business.slug(),
                        business.timezone().value().getId()),
                new CustomerReservationResult.ResourceSummary(
                        resource.id().value(),
                        resource.name().value(),
                        resource.slug().value(),
                        resource.status() == ResourceStatus.ACTIVE),
                reservation.startAt(),
                reservation.endAt(),
                reservation.stateAt(now),
                reservation.holdExpiresAt(),
                reservation.createdAt(),
                reservation.updatedAt(),
                business.timezone().value());
    }

    private ReservationNotFoundException customerReservationNotFound(
            final ReservationId reservationId, final CustomerReservationDenialFact denialFact) {
        LOGGER.info(
                "Customer reservation detail denied: reservationId={}, fact={}",
                reservationId.value(),
                denialFact);
        return new ReservationNotFoundException(reservationId);
    }

    private BookingContext loadBookingContext(
            final BusinessId businessId, final ResourceId resourceId) {
        final var business =
                businessLookupPort
                        .findActiveById(businessId)
                        .orElseThrow(() -> new BusinessNotAvailableException(businessId));
        final var settings =
                settingsQueryPort
                        .findByBusinessId(businessId)
                        .orElseThrow(() -> new BookingSettingsRequiredException(businessId));
        final var resource =
                resourceQueryPort
                        .findByBusinessIdAndId(businessId, resourceId)
                        .filter(value -> value.status() == ResourceStatus.ACTIVE)
                        .orElseThrow(
                                () -> new ResourceNotAvailableException(businessId, resourceId));
        return new BookingContext(business, resource.bookingOverrides().resolve(settings));
    }

    private Slot decodeAndValidateSlot(
            final HoldReservationCommand command, final BookingContext context, final Instant now) {
        final var decoded = new SlotId(command.slotId()).decode();
        if (!decoded.businessId().equals(command.businessId().value())
                || !decoded.resourceId().equals(command.resourceId().value())
                || !decoded.startAt().isAfter(now)) {
            throw new SlotUnavailableException(command.resourceId(), decoded.startAt());
        }

        final var date =
                LocalDate.ofInstant(decoded.startAt(), context.business().timezone().value());
        final var today = LocalDate.now(clock.withZone(context.business().timezone().value()));
        if (date.isBefore(today)
                || date.isAfter(today.plusDays(context.policy().maxAdvanceBookingDays().days()))) {
            throw new SlotUnavailableException(command.resourceId(), decoded.startAt());
        }

        return generatedSlots(command.businessId(), command.resourceId(), date, context).stream()
                .filter(
                        slot ->
                                slot.startAt().equals(decoded.startAt())
                                        && slot.endAt().equals(decoded.endAt()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new SlotUnavailableException(
                                        command.resourceId(), decoded.startAt()));
    }

    private List<Slot> generatedSlots(
            final BusinessId businessId,
            final ResourceId resourceId,
            final LocalDate date,
            final BookingContext context) {
        final var windows =
                scheduleQueryPort
                        .findDateOverride(businessId, resourceId, date)
                        .map(DateResourceScheduleOverride::windows)
                        .orElseGet(
                                () ->
                                        scheduleQueryPort
                                                .findWeekly(
                                                        businessId, resourceId, date.getDayOfWeek())
                                                .map(WeeklyResourceSchedule::windows)
                                                .orElse(List.of()));
        return SlotGenerator.generate(
                businessId,
                resourceId,
                context.business().timezone(),
                date,
                context.policy().slotDuration(),
                windows);
    }

    private void requireCustomerOwner(final Reservation reservation, final AccountId accountId) {
        if (!reservation.customerAccountId().equals(accountId)) {
            throw new ReservationAccessDeniedException();
        }
    }

    private void requireBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        if (!businessAccessPort.hasBusinessAccess(accountId, businessId)) {
            throw new ReservationAccessDeniedException();
        }
    }

    private record BookingContext(
            BusinessLookupPort.BusinessView business, EffectiveBookingPolicy policy) {}
}

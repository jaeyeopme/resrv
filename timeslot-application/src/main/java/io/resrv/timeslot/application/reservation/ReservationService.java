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
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
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
import io.resrv.timeslot.application.slot.VirtualSlotService;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;
import io.resrv.timeslot.domain.slot.Slot;
import io.resrv.timeslot.domain.slot.SlotId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

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

        final var holdTtl = effectiveHoldTtl(context.settings().holdTtl(), context.resource());
        final var reservation =
                Reservation.hold(
                        command.businessId(),
                        command.resourceId(),
                        command.accountId(),
                        slot.startAt(),
                        slot.endAt(),
                        now.plusSeconds(holdTtl.minutes() * 60L),
                        now);
        reservationCommandPort.save(reservation);
        return ReservationResult.from(reservation, now);
    }

    public ReservationResult confirm(final ConfirmReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireCustomerOwner(reservation, command.accountId());
        final var confirmed = reservation.confirm(now);
        reservationCommandPort.save(confirmed);
        return ReservationResult.from(confirmed, now);
    }

    public ReservationResult release(final ReleaseReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireCustomerOwner(reservation, command.accountId());
        final var released = reservation.release(now);
        reservationCommandPort.save(released);
        return ReservationResult.from(released, now);
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
        return ReservationResult.from(cancelled, now);
    }

    public ReservationResult checkIn(final CheckInReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireBusinessAccess(command.accountId(), command.businessId());
        final var checkedIn = reservation.checkIn(now);
        reservationCommandPort.save(checkedIn);
        return ReservationResult.from(checkedIn, now);
    }

    public ReservationResult markNoShow(final MarkNoShowReservationCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var now = clock.instant();
        final var reservation = findLocked(command.businessId(), command.reservationId());
        requireBusinessAccess(command.accountId(), command.businessId());
        final var noShow = reservation.markNoShow(now);
        reservationCommandPort.save(noShow);
        return ReservationResult.from(noShow, now);
    }

    private Reservation cancelByCustomer(
            final CancelReservationCommand command,
            final Reservation reservation,
            final Instant now) {
        requireCustomerOwner(reservation, command.accountId());
        final var context = loadBookingContext(command.businessId(), reservation.resourceId());
        final var cancellationWindow =
                effectiveCancellationWindow(
                        context.settings().cancellationWindow(), context.resource());
        final var cutoff =
                reservation.startAt().minus(Duration.ofMinutes(cancellationWindow.minutes()));
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
        return new BookingContext(business, settings, resource);
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
                || date.isAfter(
                        today.plusDays(context.settings().maxAdvanceBookingDays().days()))) {
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
                        .map(value -> value.windows())
                        .orElseGet(
                                () ->
                                        scheduleQueryPort
                                                .findWeekly(
                                                        businessId, resourceId, date.getDayOfWeek())
                                                .map(value -> value.windows())
                                                .orElse(List.of()));
        return VirtualSlotService.generateSlots(
                businessId,
                resourceId,
                context.business().timezone(),
                date,
                effectiveSlotDuration(context.settings().slotDuration(), context.resource()),
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

    private static SlotDuration effectiveSlotDuration(
            final SlotDuration defaultSlotDuration, final Resource resource) {
        final var override = resource.bookingOverrides().slotDuration();
        return override == null ? defaultSlotDuration : override;
    }

    private static HoldTtl effectiveHoldTtl(final HoldTtl defaultHoldTtl, final Resource resource) {
        final var override = resource.bookingOverrides().holdTtl();
        return override == null ? defaultHoldTtl : override;
    }

    private static CancellationWindow effectiveCancellationWindow(
            final CancellationWindow defaultCancellationWindow, final Resource resource) {
        final var override = resource.bookingOverrides().cancellationWindow();
        return override == null ? defaultCancellationWindow : override;
    }

    private record BookingContext(
            BusinessLookupPort.BusinessView business,
            BusinessBookingSettings settings,
            Resource resource) {}
}

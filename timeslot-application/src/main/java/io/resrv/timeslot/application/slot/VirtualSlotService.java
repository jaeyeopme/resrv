package io.resrv.timeslot.application.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.resource.ResourceNotAvailableException;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.application.slot.in.ListSlotsQuery;
import io.resrv.timeslot.application.slot.in.ListSlotsUseCase;
import io.resrv.timeslot.application.slot.in.SlotResult;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.MaxAdvanceBookingDays;
import io.resrv.timeslot.domain.settings.SlotDuration;
import io.resrv.timeslot.domain.slot.Slot;
import io.resrv.timeslot.domain.slot.SlotId;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VirtualSlotService implements ListSlotsUseCase {

    private final BusinessLookupPort businessLookupPort;
    private final BusinessBookingSettingsQueryPort settingsQueryPort;
    private final ResourceQueryPort resourceQueryPort;
    private final ResourceScheduleQueryPort scheduleQueryPort;
    private final Clock clock;

    public VirtualSlotService(
            final BusinessLookupPort businessLookupPort,
            final BusinessBookingSettingsQueryPort settingsQueryPort,
            final ResourceQueryPort resourceQueryPort,
            final ResourceScheduleQueryPort scheduleQueryPort,
            final Clock clock) {
        this.businessLookupPort = businessLookupPort;
        this.settingsQueryPort = settingsQueryPort;
        this.resourceQueryPort = resourceQueryPort;
        this.scheduleQueryPort = scheduleQueryPort;
        this.clock = clock;
    }

    @Override
    public List<SlotResult> listSlots(final ListSlotsQuery query) {
        final var business =
                businessLookupPort
                        .findActiveById(query.businessId())
                        .orElseThrow(() -> new BusinessNotAvailableException(query.businessId()));
        final var settings =
                settingsQueryPort
                        .findByBusinessId(query.businessId())
                        .orElseThrow(
                                () -> new BookingSettingsRequiredException(query.businessId()));
        final var resource =
                resourceQueryPort
                        .findByBusinessIdAndId(query.businessId(), query.resourceId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotAvailableException(
                                                query.businessId(), query.resourceId()));

        final var today = LocalDate.now(clock.withZone(business.timezone().value()));
        if (query.date().isBefore(today)
                || query.date().isAfter(today.plusDays(settings.maxAdvanceBookingDays().days()))) {
            return List.of();
        }

        final var windows =
                scheduleQueryPort
                        .findDateOverride(query.businessId(), query.resourceId(), query.date())
                        .map(value -> value.windows())
                        .orElseGet(
                                () ->
                                        scheduleQueryPort
                                                .findWeekly(
                                                        query.businessId(),
                                                        query.resourceId(),
                                                        query.date().getDayOfWeek())
                                                .map(value -> value.windows())
                                                .orElse(List.of()));
        final var effectiveSlotDuration =
                effectiveSlotDuration(settings.slotDuration(), resource.bookingOverrides());

        return generateSlots(
                        query.businessId(),
                        query.resourceId(),
                        business.timezone(),
                        query.date(),
                        effectiveSlotDuration,
                        windows)
                .stream()
                .map(SlotResult::from)
                .toList();
    }

    public static List<Slot> generateSlots(
            final BusinessId businessId,
            final ResourceId resourceId,
            final Timezone timezone,
            final LocalDate date,
            final SlotDuration slotDuration,
            final List<ScheduleWindow> windows) {
        final var slots = new ArrayList<Slot>();
        for (final var window : windows) {
            var slotStart = date.atTime(window.startTime());
            final var localEnd = date.atTime(window.endTime());
            while (!slotStart.plusMinutes(slotDuration.minutes()).isAfter(localEnd)) {
                final var slotEnd = slotStart.plusMinutes(slotDuration.minutes());
                slots.add(toSlot(businessId, resourceId, timezone, slotStart, slotEnd));
                slotStart = slotEnd;
            }
        }
        return List.copyOf(slots);
    }

    private static Slot toSlot(
            final BusinessId businessId,
            final ResourceId resourceId,
            final Timezone timezone,
            final LocalDateTime localStart,
            final LocalDateTime localEnd) {
        final var startAtBusinessTime = localStart.atZone(timezone.value()).toOffsetDateTime();
        final var endAtBusinessTime = localEnd.atZone(timezone.value()).toOffsetDateTime();
        final var startAt = startAtBusinessTime.toInstant();
        final var endAt = endAtBusinessTime.toInstant();
        return new Slot(
                SlotId.of(businessId, resourceId, startAt, endAt),
                businessId,
                resourceId,
                startAt,
                endAt,
                startAtBusinessTime,
                endAtBusinessTime);
    }

    private static SlotDuration effectiveSlotDuration(
            final SlotDuration defaultSlotDuration, final ResourceBookingOverrides overrides) {
        return overrides.slotDuration() == null ? defaultSlotDuration : overrides.slotDuration();
    }

    @SuppressWarnings("unused")
    private static HoldTtl effectiveHoldTtl(
            final HoldTtl defaultHoldTtl, final ResourceBookingOverrides overrides) {
        return overrides.holdTtl() == null ? defaultHoldTtl : overrides.holdTtl();
    }

    @SuppressWarnings("unused")
    private static CancellationWindow effectiveCancellationWindow(
            final CancellationWindow defaultCancellationWindow,
            final ResourceBookingOverrides overrides) {
        return overrides.cancellationWindow() == null
                ? defaultCancellationWindow
                : overrides.cancellationWindow();
    }

    @SuppressWarnings("unused")
    private static MaxAdvanceBookingDays effectiveMaxAdvanceBookingDays(
            final BusinessBookingSettings settings) {
        return settings.maxAdvanceBookingDays();
    }
}

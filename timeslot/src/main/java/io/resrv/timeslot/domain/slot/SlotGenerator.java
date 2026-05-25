package io.resrv.timeslot.domain.slot;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SlotGenerator {

    private SlotGenerator() {}

    public static List<Slot> generate(
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
}

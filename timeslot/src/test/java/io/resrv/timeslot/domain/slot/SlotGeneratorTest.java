package io.resrv.timeslot.domain.slot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SlotGeneratorTest {

    @Test
    void generatesSlotsFromMultipleWindowsInBusinessTimezone() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var slots =
                SlotGenerator.generate(
                        businessId,
                        resourceId,
                        Timezone.of("Asia/Seoul"),
                        LocalDate.parse("2026-05-25"),
                        new SlotDuration(30),
                        List.of(
                                new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                                new ScheduleWindow(LocalTime.of(14, 0), LocalTime.of(15, 0))));

        assertEquals(4, slots.size());
        assertEquals(Instant.parse("2026-05-25T00:00:00Z"), slots.getFirst().startAt());
        assertEquals("2026-05-25T09:00+09:00", slots.getFirst().startAtBusinessTime().toString());
        assertEquals(
                SlotId.of(
                                businessId,
                                resourceId,
                                slots.getFirst().startAt(),
                                slots.getFirst().endAt())
                        .value(),
                slots.getFirst().id().value());
    }
}

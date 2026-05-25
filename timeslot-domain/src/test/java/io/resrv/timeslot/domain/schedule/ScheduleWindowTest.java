package io.resrv.timeslot.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleWindowTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void acceptsSameDayWindow() {
        assertDoesNotThrow(() -> new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(18, 0)));
    }

    @Test
    void rejectsOvernightWindow() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ScheduleWindow(LocalTime.of(22, 0), LocalTime.of(2, 0)));

        assertEquals("Schedule window must start and end on the same date", exception.getMessage());
    }

    @Test
    void rejectsZeroLengthWindow() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(9, 0)));

        assertEquals("Schedule window must start and end on the same date", exception.getMessage());
    }

    @Test
    void rejectsOverlappingWindowsAfterSorting() {
        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                ScheduleWindow.validateNoOverlap(
                                        List.of(
                                                new ScheduleWindow(
                                                        LocalTime.of(13, 0), LocalTime.of(14, 0)),
                                                new ScheduleWindow(
                                                        LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                                new ScheduleWindow(
                                                        LocalTime.of(11, 59),
                                                        LocalTime.of(13, 0)))));

        assertEquals("Schedule windows must not overlap", exception.getMessage());
    }

    @Test
    void allowsAdjacentWindows() {
        assertDoesNotThrow(
                () ->
                        ScheduleWindow.validateNoOverlap(
                                List.of(
                                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                        new ScheduleWindow(
                                                LocalTime.of(12, 0), LocalTime.of(15, 0)))));
    }

    @Test
    void nullWindowListFailsClearly() {
        final var exception =
                assertThrows(
                        NullPointerException.class, () -> ScheduleWindow.validateNoOverlap(null));

        assertEquals("Schedule windows must not be null", exception.getMessage());
    }

    @Test
    void nullWindowInListFailsClearly() {
        final var windows =
                Arrays.asList(new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)), null);

        final var exception =
                assertThrows(
                        NullPointerException.class,
                        () -> ScheduleWindow.validateNoOverlap(windows));

        assertEquals("Schedule window must not be null", exception.getMessage());
    }

    @Test
    void weeklyScheduleAllowsEmptyWindowsForClosedDay() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();

        final var schedule =
                WeeklyResourceSchedule.create(
                        businessId, resourceId, DayOfWeek.MONDAY, List.of(), CREATED_AT);

        assertEquals(businessId, schedule.businessId());
        assertEquals(resourceId, schedule.resourceId());
        assertEquals(DayOfWeek.MONDAY, schedule.dayOfWeek());
        assertEquals(List.of(), schedule.windows());
        assertEquals(CREATED_AT, schedule.createdAt());
        assertEquals(CREATED_AT, schedule.updatedAt());
    }

    @Test
    void weeklyScheduleDefensivelyCopiesWindows() {
        final var windows =
                new ArrayList<>(
                        List.of(new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0))));

        final var schedule =
                WeeklyResourceSchedule.create(
                        BusinessId.create(),
                        ResourceId.create(),
                        DayOfWeek.MONDAY,
                        windows,
                        CREATED_AT);
        windows.clear();

        assertEquals(1, schedule.windows().size());
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        schedule.windows()
                                .add(new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(14, 0))));
    }

    @Test
    void weeklyScheduleReplacementPreservesIdentityAndCreatedAt() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var existing =
                WeeklyResourceSchedule.reconstitute(
                        businessId,
                        resourceId,
                        DayOfWeek.TUESDAY,
                        List.of(new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                        CREATED_AT,
                        CREATED_AT);

        final var updated =
                existing.replaceWindows(
                        List.of(new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                        UPDATED_AT);

        assertEquals(businessId, updated.businessId());
        assertEquals(resourceId, updated.resourceId());
        assertEquals(DayOfWeek.TUESDAY, updated.dayOfWeek());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(UPDATED_AT, updated.updatedAt());
        assertEquals(
                List.of(new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                updated.windows());
    }

    @Test
    void dateOverrideAllowsEmptyWindowsForClosedDay() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var date = LocalDate.parse("2026-01-05");

        final var override =
                DateResourceScheduleOverride.create(
                        businessId, resourceId, date, List.of(), CREATED_AT);

        assertEquals(businessId, override.businessId());
        assertEquals(resourceId, override.resourceId());
        assertEquals(date, override.date());
        assertEquals(List.of(), override.windows());
        assertEquals(CREATED_AT, override.createdAt());
        assertEquals(CREATED_AT, override.updatedAt());
    }

    @Test
    void dateOverrideReplacementPreservesIdentityAndCreatedAt() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var date = LocalDate.parse("2026-01-05");
        final var existing =
                DateResourceScheduleOverride.reconstitute(
                        businessId,
                        resourceId,
                        date,
                        List.of(new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                        CREATED_AT,
                        CREATED_AT);

        final var updated =
                existing.replaceWindows(
                        List.of(new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                        UPDATED_AT);

        assertEquals(businessId, updated.businessId());
        assertEquals(resourceId, updated.resourceId());
        assertEquals(date, updated.date());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(UPDATED_AT, updated.updatedAt());
        assertEquals(
                List.of(new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))),
                updated.windows());
    }

    @Test
    void scheduleRecordsRejectOverlappingWindows() {
        final var windows =
                List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new ScheduleWindow(LocalTime.of(11, 0), LocalTime.of(13, 0)));

        final var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                WeeklyResourceSchedule.create(
                                        BusinessId.create(),
                                        ResourceId.create(),
                                        DayOfWeek.MONDAY,
                                        windows,
                                        CREATED_AT));

        assertEquals("Schedule windows must not overlap", exception.getMessage());
    }
}

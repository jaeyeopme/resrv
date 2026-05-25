package io.resrv.timeslot.domain.schedule;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ScheduleWindow(LocalTime startTime, LocalTime endTime) {

    public ScheduleWindow {
        Objects.requireNonNull(startTime, "Schedule window start time must not be null");
        Objects.requireNonNull(endTime, "Schedule window end time must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "Schedule window must start and end on the same date");
        }
    }

    public static void validateNoOverlap(final List<ScheduleWindow> windows) {
        Objects.requireNonNull(windows, "Schedule windows must not be null");
        windows.forEach(
                window -> Objects.requireNonNull(window, "Schedule window must not be null"));

        final var sorted =
                windows.stream().sorted(Comparator.comparing(ScheduleWindow::startTime)).toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                throw new IllegalArgumentException("Schedule windows must not overlap");
            }
        }
    }
}

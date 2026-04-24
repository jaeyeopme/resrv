package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlotDurationTest {

    @ParameterizedTest
    @ValueSource(ints = {30, 60, 90, 120, 480})
    void validDurations(final int minutes) {
        final var duration = new SlotDuration(minutes);
        assertEquals(minutes, duration.minutes());
    }

    @Test
    void rejectBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new SlotDuration(20));
    }

    @Test
    void rejectAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new SlotDuration(510));
    }

    @ParameterizedTest
    @ValueSource(ints = {45, 50, 75, 100})
    void rejectNon30MinuteMultiples(final int minutes) {
        assertThrows(IllegalArgumentException.class, () -> new SlotDuration(minutes));
    }
}

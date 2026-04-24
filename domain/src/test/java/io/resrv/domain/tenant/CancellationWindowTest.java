package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CancellationWindowTest {

    @Test
    void validZero() {
        final var window = new CancellationWindow(0);
        assertEquals(0, window.minutes());
    }

    @Test
    void validPositive() {
        final var window = new CancellationWindow(120);
        assertEquals(120, window.minutes());
    }

    @Test
    void rejectNegative() {
        assertThrows(IllegalArgumentException.class, () -> new CancellationWindow(-1));
    }
}

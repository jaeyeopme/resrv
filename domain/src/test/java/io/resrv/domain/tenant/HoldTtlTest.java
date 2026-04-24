package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HoldTtlTest {

    @Test
    void validMinimum() {
        final var ttl = new HoldTtl(5);
        assertEquals(5, ttl.minutes());
    }

    @Test
    void validMaximum() {
        final var ttl = new HoldTtl(30);
        assertEquals(30, ttl.minutes());
    }

    @Test
    void rejectBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new HoldTtl(4));
    }

    @Test
    void rejectAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new HoldTtl(31));
    }
}

package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TimezoneTest {

    @Test
    void validTimezone() {
        final var timezone = new Timezone(ZoneId.of("Asia/Seoul"));
        assertEquals(ZoneId.of("Asia/Seoul"), timezone.value());
    }

    @Test
    void utcTimezone() {
        final var timezone = new Timezone(ZoneId.of("UTC"));
        assertEquals(ZoneId.of("UTC"), timezone.value());
    }

    @Test
    void rejectNull() {
        assertThrows(NullPointerException.class, () -> new Timezone(null));
    }
}

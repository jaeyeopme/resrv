package io.resrv.shared.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

final class TimezoneTest {

    @Test
    void acceptsIanaTimezone() {
        assertEquals(ZoneId.of("Asia/Seoul"), Timezone.of("Asia/Seoul").value());
    }

    @Test
    void rejectsBlankTimezone() {
        assertThrows(IllegalArgumentException.class, () -> Timezone.of(" "));
    }

    @Test
    void rejectsNullTimezoneWithBlankMessage() {
        final var error = assertThrows(IllegalArgumentException.class, () -> Timezone.of(null));
        assertEquals("Timezone must not be blank", error.getMessage());
    }

    @Test
    void rejectsNullZoneId() {
        final var error = assertThrows(NullPointerException.class, () -> new Timezone(null));
        assertEquals("Timezone must not be null", error.getMessage());
    }
}

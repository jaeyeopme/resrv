package io.resrv.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void validEmail() {
        final var email = new Email("admin@example.com");
        assertEquals("admin@example.com", email.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "admin@", "@example.com", "admin@.com"})
    void rejectInvalidFormats(final String value) {
        assertThrows(IllegalArgumentException.class, () -> new Email(value));
    }

    @Test
    void rejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
    }
}

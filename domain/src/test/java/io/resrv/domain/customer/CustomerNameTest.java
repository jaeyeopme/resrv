package io.resrv.domain.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerNameTest {

    @Test
    void normalizesName() {
        final var name = new CustomerName("  Jane Customer  ");

        assertEquals("Jane Customer", name.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(final String value) {
        assertThrows(IllegalArgumentException.class, () -> new CustomerName(value));
    }

    @Test
    void rejectsLongName() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerName("a".repeat(101)));
    }
}

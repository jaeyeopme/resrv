package io.resrv.domain.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerEmailTest {

    @Test
    void normalizesEmail() {
        final var email = new CustomerEmail(" Customer@Example.COM ");

        assertEquals("customer@example.com", email.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "invalid", "missing-at.example.com", "name@domain"})
    void rejectsInvalidEmail(final String value) {
        assertThrows(IllegalArgumentException.class, () -> new CustomerEmail(value));
    }
}

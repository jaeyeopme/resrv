package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlugTest {

    @Test
    void validSlug() {
        final var slug = new Slug("my-salon");
        assertEquals("my-salon", slug.value());
    }

    @Test
    void minimumLength() {
        final var slug = new Slug("abc");
        assertEquals("abc", slug.value());
    }

    @Test
    void maximumLength() {
        final var slug = new Slug("a".repeat(63));
        assertEquals(63, slug.value().length());
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("ab"));
    }

    @Test
    void tooLong() {
        final var tooLongValue = "a".repeat(64);
        assertThrows(IllegalArgumentException.class, () -> new Slug(tooLongValue));
    }

    @ParameterizedTest
    @ValueSource(strings = {"My-Salon", "MY-SALON", "mySalon"})
    void rejectUppercase(final String value) {
        assertThrows(IllegalArgumentException.class, () -> new Slug(value));
    }

    @Test
    void rejectConsecutiveHyphens() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("my--salon"));
    }

    @Test
    void rejectStartWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("-my-salon"));
    }

    @Test
    void rejectEndWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("my-salon-"));
    }

    @Test
    void rejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Slug(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "a1b", "salon-123", "my-great-salon"})
    void acceptValidFormats(final String value) {
        assertDoesNotThrow(() -> new Slug(value));
    }
}

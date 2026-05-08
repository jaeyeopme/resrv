package io.resrv.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceSlugTest {

    @Test
    void validSlug_isAccepted() {
        assertEquals("room-101", new ResourceSlug("room-101").value());
    }

    @Test
    void shortSlug_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("ab"));
    }

    @Test
    void uppercaseSlug_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("Room-101"));
    }

    @Test
    void consecutiveHyphen_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("room--101"));
    }

    @Test
    void leadingHyphen_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("-room"));
    }

    @Test
    void maxLengthSlug_isAccepted() {
        final var slug = "a" + "-a".repeat(31);

        assertEquals(slug, new ResourceSlug(slug).value());
    }

    @Test
    void tooLongSlug_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("a".repeat(64)));
    }

    @Test
    void trailingHyphen_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceSlug("room-"));
    }

    @Test
    void nullSlug_throws() {
        assertThrows(NullPointerException.class, () -> new ResourceSlug(null));
    }
}

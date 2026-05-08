package io.resrv.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceNameTest {

    @Test
    void trimsName() {
        assertEquals("Room A", new ResourceName("  Room A  ").value());
    }

    @Test
    void blankName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceName("   "));
    }

    @Test
    void tooLongName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceName("a".repeat(101)));
    }

    @Test
    void maxLengthName_isAccepted() {
        assertEquals("a".repeat(100), new ResourceName("a".repeat(100)).value());
    }

    @Test
    void nullName_throws() {
        assertThrows(NullPointerException.class, () -> new ResourceName(null));
    }
}

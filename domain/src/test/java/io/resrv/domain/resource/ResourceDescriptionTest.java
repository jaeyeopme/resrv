package io.resrv.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceDescriptionTest {

    @Test
    void trimsDescription() {
        assertEquals("Quiet room", new ResourceDescription("  Quiet room  ").value());
    }

    @Test
    void blankDescription_becomesNull() {
        assertNull(new ResourceDescription("   ").value());
    }

    @Test
    void nullDescription_isAllowed() {
        assertNull(new ResourceDescription(null).value());
    }

    @Test
    void tooLongDescription_throws() {
        assertThrows(
                IllegalArgumentException.class, () -> new ResourceDescription("a".repeat(501)));
    }

    @Test
    void maxLengthDescription_isAccepted() {
        assertEquals("a".repeat(500), new ResourceDescription("a".repeat(500)).value());
    }

    @Test
    void emptyFactory_returnsNullDescription() {
        assertNull(ResourceDescription.empty().value());
    }
}

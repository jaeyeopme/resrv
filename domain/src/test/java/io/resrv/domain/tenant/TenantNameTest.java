package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TenantNameTest {

    @Test
    void validName() {
        final var name = new TenantName("마이살롱");
        assertEquals("마이살롱", name.value());
    }

    @Test
    void minimumLength() {
        final var name = new TenantName("A");
        assertEquals("A", name.value());
    }

    @Test
    void maximumLength() {
        final var name = new TenantName("A".repeat(100));
        assertEquals(100, name.value().length());
    }

    @Test
    void tooLong() {
        final var tooLongValue = "A".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> new TenantName(tooLongValue));
    }

    @Test
    void rejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> new TenantName("   "));
    }

    @Test
    void trimWhitespace() {
        final var name = new TenantName("  마이살롱  ");
        assertEquals("마이살롱", name.value());
    }
}

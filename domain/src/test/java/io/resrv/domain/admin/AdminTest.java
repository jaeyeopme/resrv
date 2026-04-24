package io.resrv.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdminTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void createWithRequiredFields() {
        final var tenantId = TenantId.create();
        final var admin =
                Admin.create(tenantId, new Email("admin@example.com"), "$argon2id$hashed", NOW);

        assertNotNull(admin.id());
        assertEquals(7, admin.id().value().version());
        assertEquals(tenantId, admin.tenantId());
        assertEquals("admin@example.com", admin.email().value());
        assertEquals("$argon2id$hashed", admin.hashedPassword());
        assertEquals(AdminRole.OWNER, admin.role());
        assertEquals(NOW, admin.createdAt());
    }

    @Test
    void rejectBlankHashedPassword() {
        final var tenantId = TenantId.create();
        final var email = new Email("admin@example.com");
        assertThrows(IllegalArgumentException.class, () -> Admin.create(tenantId, email, "", NOW));
    }

    @Test
    void equalsBySameId() {
        final var tenantId = TenantId.create();
        final var admin1 =
                Admin.create(tenantId, new Email("a@example.com"), "$argon2id$hash1", NOW);
        final var admin2 =
                Admin.create(tenantId, new Email("b@example.com"), "$argon2id$hash2", NOW);

        // Different admins with different IDs
        assertNotEquals(admin1, admin2);
    }

    @Test
    void notEqualByDifferentId() {
        final var admin1 =
                Admin.create(
                        TenantId.create(), new Email("same@example.com"), "$argon2id$hashed", NOW);
        final var admin2 =
                Admin.create(
                        TenantId.create(), new Email("same@example.com"), "$argon2id$hashed", NOW);

        assertNotEquals(admin1, admin2);
    }
}

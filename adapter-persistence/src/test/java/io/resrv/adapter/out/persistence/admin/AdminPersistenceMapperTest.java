package io.resrv.adapter.out.persistence.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.resrv.domain.admin.Admin;
import io.resrv.domain.admin.AdminId;
import io.resrv.domain.admin.AdminRole;
import io.resrv.domain.admin.Email;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdminPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void toJpaEntity_mapsAdminFields() {
        final var tenantId = TenantId.create();
        final var admin =
                Admin.reconstitute(
                        AdminId.create(),
                        tenantId,
                        new Email("staff@example.com"),
                        "$argon2id$hashed",
                        AdminRole.STAFF,
                        false,
                        CREATED_AT);

        final var entity = AdminPersistenceMapper.toJpaEntity(admin);

        assertEquals(admin.id().value(), entity.getId());
        assertEquals(tenantId.value(), entity.getTenantId());
        assertEquals("staff@example.com", entity.getEmail());
        assertEquals("$argon2id$hashed", entity.getHashedPassword());
        assertEquals(AdminRole.STAFF.name(), entity.getRole());
        assertFalse(entity.isActive());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }
}

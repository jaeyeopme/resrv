package io.resrv.adapter.out.persistence.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.admin.Admin;
import io.resrv.domain.admin.AdminRole;
import io.resrv.domain.admin.Email;
import io.resrv.domain.tenant.TenantId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AdminPersistenceAdapter.class)
class AdminPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private AdminPersistenceAdapter adminAdapter;

    @Autowired private AdminJpaRepository adminRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndRetrieve() {
        final var tenantId = insertTenantDirectly();
        final var admin =
                Admin.create(tenantId, new Email("admin@example.com"), "$argon2id$hashed", NOW);

        adminAdapter.save(admin);

        final var found = adminRepository.findById(admin.id().value());
        assertTrue(found.isPresent());
        assertEquals("admin@example.com", found.get().getEmail());
        assertEquals("$argon2id$hashed", found.get().getHashedPassword());
        assertEquals(AdminRole.OWNER.name(), found.get().getRole());
    }

    @Test
    void verifyTenantIdForeignKey() {
        final var tenantId = insertTenantDirectly();
        final var admin =
                Admin.create(tenantId, new Email("admin@example.com"), "$argon2id$hashed", NOW);

        adminAdapter.save(admin);

        final var found = adminRepository.findById(admin.id().value());
        assertTrue(found.isPresent());
        assertEquals(tenantId.value(), found.get().getTenantId());
    }

    private TenantId insertTenantDirectly() {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tenant (id, name, slug, timezone, slot_duration, hold_ttl, cancellation_window, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "Test Tenant",
                "test-slug-" + System.nanoTime() % 100000,
                "UTC",
                30,
                5,
                0,
                Timestamp.from(NOW));
        return TenantId.of(id);
    }
}

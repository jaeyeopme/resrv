package io.resrv.adapter.out.persistence;

import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class PersistenceTestFixtures {

    private PersistenceTestFixtures() {}

    public static TenantId insertTenantDirectly(
            final JdbcTemplate jdbcTemplate, final Instant createdAt, final String slugPrefix) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tenant (id, name, slug, timezone, slot_duration, hold_ttl, cancellation_window, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "Test Tenant",
                slugPrefix + "-" + Math.abs(System.nanoTime()),
                "UTC",
                30,
                5,
                0,
                Timestamp.from(createdAt));
        return TenantId.of(id);
    }

    public static ResourceId insertResourceDirectly(
            final JdbcTemplate jdbcTemplate,
            final Instant createdAt,
            final TenantId tenantId,
            final String slug) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO resource (id, tenant_id, slug, name, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                tenantId.value(),
                slug,
                "Room " + slug,
                "Quiet",
                "ACTIVE",
                Timestamp.from(createdAt),
                Timestamp.from(createdAt));
        return ResourceId.of(id);
    }
}

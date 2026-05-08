package io.resrv.adapter.out.persistence.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceSlugAlreadyExistsException;
import io.resrv.domain.resource.ResourceStatus;
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
@Import(ResourcePersistenceAdapter.class)
class ResourcePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private ResourcePersistenceAdapter adapter;

    @Autowired private ResourceJpaRepository repository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndRetrieve() {
        final var tenantId = insertTenantDirectly("resource-save");
        final var resource = createResource(tenantId, "room-a");

        adapter.save(resource);

        final var found = repository.findById(resource.id().value());
        assertTrue(found.isPresent());
        assertEquals(tenantId.value(), found.get().getTenantId());
        assertEquals("room-a", found.get().getSlug());
        assertEquals("Room room-a", found.get().getName());
    }

    @Test
    void sameSlugInSameTenant_throwsResourceSlugAlreadyExistsException() {
        final var tenantId = insertTenantDirectly("resource-duplicate");
        adapter.save(createResource(tenantId, "room-a"));

        assertThrows(
                ResourceSlugAlreadyExistsException.class,
                () -> adapter.save(createResource(tenantId, "room-a")));
    }

    @Test
    void sameSlugInDifferentTenants_isAllowed() {
        final var firstTenantId = insertTenantDirectly("resource-first");
        final var secondTenantId = insertTenantDirectly("resource-second");

        adapter.save(createResource(firstTenantId, "room-a"));
        adapter.save(createResource(secondTenantId, "room-a"));

        assertEquals(2, repository.count());
    }

    @Test
    void findByTenantIdAndId_scopesByTenant() {
        final var firstTenantId = insertTenantDirectly("resource-scope-first");
        final var secondTenantId = insertTenantDirectly("resource-scope-second");
        final var resource = createResource(firstTenantId, "room-a");
        adapter.save(resource);

        assertTrue(adapter.findByTenantIdAndId(firstTenantId, resource.id()).isPresent());
        assertFalse(adapter.findByTenantIdAndId(secondTenantId, resource.id()).isPresent());
    }

    @Test
    void existsByTenantIdAndSlug_scopesByTenant() {
        final var firstTenantId = insertTenantDirectly("resource-exists-first");
        final var secondTenantId = insertTenantDirectly("resource-exists-second");
        adapter.save(createResource(firstTenantId, "room-a"));

        assertTrue(adapter.existsByTenantIdAndSlug(firstTenantId, new ResourceSlug("room-a")));
        assertFalse(adapter.existsByTenantIdAndSlug(secondTenantId, new ResourceSlug("room-a")));
    }

    @Test
    void findByTenantIdAndStatus_returnsOnlyRequestedStatusForTenant() {
        final var tenantId = insertTenantDirectly("resource-status");
        final var otherTenantId = insertTenantDirectly("resource-status-other");
        adapter.save(createResource(tenantId, "active-room"));
        adapter.save(createResource(tenantId, "inactive-room").deactivate(NOW.plusSeconds(60)));
        adapter.save(createResource(otherTenantId, "other-room"));

        final var activeResources =
                adapter.findByTenantIdAndStatus(tenantId, ResourceStatus.ACTIVE);

        assertEquals(1, activeResources.size());
        assertEquals("active-room", activeResources.getFirst().slug().value());
    }

    private TenantId insertTenantDirectly(final String slugPrefix) {
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
                Timestamp.from(NOW));
        return TenantId.of(id);
    }

    private static Resource createResource(final TenantId tenantId, final String slug) {
        return Resource.create(
                tenantId,
                new ResourceName("Room " + slug),
                new ResourceSlug(slug),
                new ResourceDescription("Quiet"),
                NOW);
    }
}

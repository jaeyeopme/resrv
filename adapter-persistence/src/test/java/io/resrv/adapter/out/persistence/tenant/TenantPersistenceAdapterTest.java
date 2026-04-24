package io.resrv.adapter.out.persistence.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.Timezone;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TenantPersistenceAdapter.class)
class TenantPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private TenantPersistenceAdapter adapter;

    @Autowired private TenantJpaRepository repository;

    @Test
    void saveAndRetrieve() {
        final var tenant =
                Tenant.create(
                        new TenantName("마이살롱"),
                        new Slug("my-salon"),
                        new Timezone(ZoneId.of("Asia/Seoul")),
                        new SlotDuration(60),
                        new HoldTtl(15),
                        new CancellationWindow(0),
                        NOW);

        adapter.save(tenant);

        final var found = repository.findById(tenant.id().value());
        assertTrue(found.isPresent());
        assertEquals("my-salon", found.get().getSlug());
        assertEquals("마이살롱", found.get().getName());
    }

    @Test
    void slugUniquenessViolation_throwsSlugAlreadyExistsException() {
        final var tenant1 =
                Tenant.create(
                        new TenantName("Salon 1"),
                        new Slug("same-slug"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        adapter.save(tenant1);

        final var tenant2 =
                Tenant.create(
                        new TenantName("Salon 2"),
                        new Slug("same-slug"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        assertThrows(SlugAlreadyExistsException.class, () -> adapter.save(tenant2));
    }

    @Test
    void existsBySlug_returnsTrue() {
        final var tenant =
                Tenant.create(
                        new TenantName("Test"),
                        new Slug("existing-slug"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        adapter.save(tenant);

        assertTrue(adapter.existsBySlug("existing-slug"));
    }

    @Test
    void existsBySlug_returnsFalse() {
        assertFalse(adapter.existsBySlug("nonexistent"));
    }
}

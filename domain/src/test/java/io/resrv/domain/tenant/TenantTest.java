package io.resrv.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TenantTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void createWithAllFields() {
        final var tenant =
                Tenant.create(
                        new TenantName("마이살롱"),
                        new Slug("my-salon"),
                        new Timezone(ZoneId.of("Asia/Seoul")),
                        new SlotDuration(60),
                        new HoldTtl(15),
                        new CancellationWindow(30),
                        NOW);

        assertNotNull(tenant.id());
        assertNotNull(tenant.id().value());
        assertEquals("마이살롱", tenant.name().value());
        assertEquals("my-salon", tenant.slug().value());
        assertEquals(60, tenant.slotDuration().minutes());
        assertEquals(15, tenant.holdTtl().minutes());
        assertEquals(30, tenant.cancellationWindow().minutes());
        assertEquals(TenantStatus.PENDING, tenant.status());
        assertEquals(NOW, tenant.createdAt());
    }

    @Test
    void idIsUuidV7() {
        final var tenant =
                Tenant.create(
                        new TenantName("Test"),
                        new Slug("test-slug"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        final var uuid = tenant.id().value();
        assertEquals(7, uuid.version());
    }

    @Test
    void reconstitute_restoresAllFields() {
        final var id = TenantId.create();
        final var createdAt = Instant.parse("2025-01-01T00:00:00Z");

        final var tenant =
                Tenant.reconstitute(
                        id,
                        new TenantName("Test"),
                        new Slug("test-slug"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(60),
                        new HoldTtl(10),
                        new CancellationWindow(5),
                        TenantStatus.ACTIVE,
                        createdAt);

        assertEquals(id, tenant.id());
        assertEquals("Test", tenant.name().value());
        assertEquals("test-slug", tenant.slug().value());
        assertEquals(60, tenant.slotDuration().minutes());
        assertEquals(10, tenant.holdTtl().minutes());
        assertEquals(5, tenant.cancellationWindow().minutes());
        assertEquals(TenantStatus.ACTIVE, tenant.status());
        assertEquals(createdAt, tenant.createdAt());
    }

    @Test
    void equalsBySameId() {
        final var tenant1 =
                Tenant.create(
                        new TenantName("A"),
                        new Slug("slug-a"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        final var tenant2 =
                Tenant.reconstitute(
                        tenant1.id(),
                        new TenantName("Different"),
                        new Slug("different-slug"),
                        new Timezone(ZoneId.of("Asia/Seoul")),
                        new SlotDuration(60),
                        new HoldTtl(10),
                        new CancellationWindow(5),
                        TenantStatus.ACTIVE,
                        NOW);

        assertEquals(tenant1, tenant2);
        assertEquals(tenant1.hashCode(), tenant2.hashCode());
    }

    @Test
    void notEqualByDifferentId() {
        final var tenant1 =
                Tenant.create(
                        new TenantName("Same"),
                        new Slug("slug-a"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        final var tenant2 =
                Tenant.create(
                        new TenantName("Same"),
                        new Slug("slug-b"),
                        new Timezone(ZoneId.of("UTC")),
                        new SlotDuration(30),
                        new HoldTtl(5),
                        new CancellationWindow(0),
                        NOW);

        assertNotEquals(tenant1, tenant2);
    }
}

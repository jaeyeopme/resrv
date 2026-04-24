package io.resrv.adapter.out.persistence.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.TenantStatus;
import io.resrv.domain.tenant.Timezone;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TenantPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void toJpaEntity_mapsTenantFields() {
        final var tenant = createTenant();

        final var entity = TenantPersistenceMapper.toJpaEntity(tenant);

        assertEquals(tenant.id().value(), entity.getId());
        assertEquals("Studio A", entity.getName());
        assertEquals("studio-a", entity.getSlug());
        assertEquals("Asia/Seoul", entity.getTimezone());
        assertEquals(60, entity.getSlotDuration());
        assertEquals(10, entity.getHoldTtl());
        assertEquals(120, entity.getCancellationWindow());
        assertEquals(TenantStatus.PENDING.name(), entity.getStatus());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    void toDomain_mapsTenantFields() {
        final var entity = TenantPersistenceMapper.toJpaEntity(createTenant());

        final var tenant = TenantPersistenceMapper.toDomain(entity);

        assertEquals(entity.getId(), tenant.id().value());
        assertEquals(entity.getName(), tenant.name().value());
        assertEquals(entity.getSlug(), tenant.slug().value());
        assertEquals(ZoneId.of(entity.getTimezone()), tenant.timezone().value());
        assertEquals(entity.getSlotDuration(), tenant.slotDuration().minutes());
        assertEquals(entity.getHoldTtl(), tenant.holdTtl().minutes());
        assertEquals(entity.getCancellationWindow(), tenant.cancellationWindow().minutes());
        assertEquals(TenantStatus.PENDING, tenant.status());
        assertEquals(entity.getCreatedAt(), tenant.createdAt());
    }

    private static Tenant createTenant() {
        return Tenant.create(
                new TenantName("Studio A"),
                new Slug("studio-a"),
                new Timezone(ZoneId.of("Asia/Seoul")),
                new SlotDuration(60),
                new HoldTtl(10),
                new CancellationWindow(120),
                CREATED_AT);
    }
}

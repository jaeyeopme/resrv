package io.resrv.adapter.out.persistence.tenant;

import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantId;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.TenantStatus;
import io.resrv.domain.tenant.Timezone;
import java.time.ZoneId;

final class TenantPersistenceMapper {

    private TenantPersistenceMapper() {}

    static TenantJpaEntity toJpaEntity(final Tenant tenant) {
        return new TenantJpaEntity(
                tenant.id().value(),
                tenant.name().value(),
                tenant.slug().value(),
                tenant.timezone().value().getId(),
                tenant.slotDuration().minutes(),
                tenant.holdTtl().minutes(),
                tenant.cancellationWindow().minutes(),
                tenant.status().name(),
                tenant.createdAt());
    }

    static Tenant toDomain(final TenantJpaEntity entity) {
        return Tenant.reconstitute(
                TenantId.of(entity.getId()),
                new TenantName(entity.getName()),
                new Slug(entity.getSlug()),
                new Timezone(ZoneId.of(entity.getTimezone())),
                new SlotDuration(entity.getSlotDuration()),
                new HoldTtl(entity.getHoldTtl()),
                new CancellationWindow(entity.getCancellationWindow()),
                TenantStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}

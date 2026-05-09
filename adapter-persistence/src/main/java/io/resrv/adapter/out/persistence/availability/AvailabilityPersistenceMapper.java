package io.resrv.adapter.out.persistence.availability;

import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.DateAvailabilityOverrideId;
import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.availability.WeeklyAvailabilityId;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;

final class AvailabilityPersistenceMapper {

    private AvailabilityPersistenceMapper() {}

    static WeeklyAvailabilityJpaEntity toJpaEntity(final WeeklyAvailability availability) {
        return new WeeklyAvailabilityJpaEntity(
                availability.id().value(),
                availability.tenantId().value(),
                availability.resourceId().value(),
                (short) availability.dayOfWeek().getValue(),
                availability.startTime(),
                availability.endTime(),
                availability.createdAt(),
                availability.updatedAt());
    }

    static DateAvailabilityOverrideJpaEntity toJpaEntity(final DateAvailabilityOverride override) {
        return new DateAvailabilityOverrideJpaEntity(
                override.id().value(),
                override.tenantId().value(),
                override.resourceId().value(),
                override.date(),
                override.closed(),
                override.startTime(),
                override.endTime(),
                override.createdAt(),
                override.updatedAt());
    }

    static WeeklyAvailability toDomain(final WeeklyAvailabilityJpaEntity entity) {
        return WeeklyAvailability.reconstitute(
                WeeklyAvailabilityId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                ResourceId.of(entity.getResourceId()),
                DayOfWeek.of(entity.getDayOfWeek()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static DateAvailabilityOverride toDomain(final DateAvailabilityOverrideJpaEntity entity) {
        return DateAvailabilityOverride.reconstitute(
                DateAvailabilityOverrideId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                ResourceId.of(entity.getResourceId()),
                entity.getDate(),
                entity.isClosed(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

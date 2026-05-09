package io.resrv.adapter.out.persistence.availability;

import io.resrv.application.availability.out.DateAvailabilityOverrideCommandPort;
import io.resrv.application.availability.out.DateAvailabilityOverrideQueryPort;
import io.resrv.application.availability.out.WeeklyAvailabilityCommandPort;
import io.resrv.application.availability.out.WeeklyAvailabilityQueryPort;
import io.resrv.domain.availability.DateAvailabilityOverride;
import io.resrv.domain.availability.WeeklyAvailability;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class AvailabilityPersistenceAdapter
        implements WeeklyAvailabilityCommandPort,
                WeeklyAvailabilityQueryPort,
                DateAvailabilityOverrideCommandPort,
                DateAvailabilityOverrideQueryPort {

    private final WeeklyAvailabilityJpaRepository weeklyAvailabilityJpaRepository;
    private final DateAvailabilityOverrideJpaRepository dateAvailabilityOverrideJpaRepository;
    private final EntityManager entityManager;

    AvailabilityPersistenceAdapter(
            final WeeklyAvailabilityJpaRepository weeklyAvailabilityJpaRepository,
            final DateAvailabilityOverrideJpaRepository dateAvailabilityOverrideJpaRepository,
            final EntityManager entityManager) {
        this.weeklyAvailabilityJpaRepository = weeklyAvailabilityJpaRepository;
        this.dateAvailabilityOverrideJpaRepository = dateAvailabilityOverrideJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final WeeklyAvailability availability) {
        weeklyAvailabilityJpaRepository.save(
                AvailabilityPersistenceMapper.toJpaEntity(availability));
        entityManager.flush();
    }

    @Override
    public void save(final DateAvailabilityOverride override) {
        dateAvailabilityOverrideJpaRepository.save(
                AvailabilityPersistenceMapper.toJpaEntity(override));
        entityManager.flush();
    }

    @Override
    public void deleteByTenantIdAndResourceIdAndDayOfWeek(
            final TenantId tenantId, final ResourceId resourceId, final DayOfWeek dayOfWeek) {
        weeklyAvailabilityJpaRepository
                .findByTenantIdAndResourceIdAndDayOfWeek(
                        tenantId.value(), resourceId.value(), (short) dayOfWeek.getValue())
                .ifPresent(weeklyAvailabilityJpaRepository::delete);
    }

    @Override
    public Optional<WeeklyAvailability> findByTenantIdAndResourceIdAndDayOfWeek(
            final TenantId tenantId, final ResourceId resourceId, final DayOfWeek dayOfWeek) {
        return weeklyAvailabilityJpaRepository
                .findByTenantIdAndResourceIdAndDayOfWeek(
                        tenantId.value(), resourceId.value(), (short) dayOfWeek.getValue())
                .map(AvailabilityPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByTenantIdAndResourceIdAndDate(
            final TenantId tenantId, final ResourceId resourceId, final LocalDate date) {
        dateAvailabilityOverrideJpaRepository
                .findByTenantIdAndResourceIdAndDate(tenantId.value(), resourceId.value(), date)
                .ifPresent(dateAvailabilityOverrideJpaRepository::delete);
    }

    @Override
    public Optional<DateAvailabilityOverride> findByTenantIdAndResourceIdAndDate(
            final TenantId tenantId, final ResourceId resourceId, final LocalDate date) {
        return dateAvailabilityOverrideJpaRepository
                .findByTenantIdAndResourceIdAndDate(tenantId.value(), resourceId.value(), date)
                .map(AvailabilityPersistenceMapper::toDomain);
    }
}

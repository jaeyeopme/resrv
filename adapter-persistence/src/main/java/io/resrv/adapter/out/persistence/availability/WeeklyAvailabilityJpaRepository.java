package io.resrv.adapter.out.persistence.availability;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface WeeklyAvailabilityJpaRepository
        extends CrudRepository<WeeklyAvailabilityJpaEntity, UUID> {

    Optional<WeeklyAvailabilityJpaEntity> findByTenantIdAndResourceIdAndDayOfWeek(
            UUID tenantId, UUID resourceId, short dayOfWeek);
}

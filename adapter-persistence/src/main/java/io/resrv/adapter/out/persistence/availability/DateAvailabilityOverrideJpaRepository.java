package io.resrv.adapter.out.persistence.availability;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface DateAvailabilityOverrideJpaRepository
        extends CrudRepository<DateAvailabilityOverrideJpaEntity, UUID> {

    Optional<DateAvailabilityOverrideJpaEntity> findByTenantIdAndResourceIdAndDate(
            UUID tenantId, UUID resourceId, LocalDate date);
}

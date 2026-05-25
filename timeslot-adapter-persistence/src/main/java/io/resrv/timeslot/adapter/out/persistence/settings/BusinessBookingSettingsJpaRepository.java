package io.resrv.timeslot.adapter.out.persistence.settings;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface BusinessBookingSettingsJpaRepository
        extends CrudRepository<BusinessBookingSettingsJpaEntity, UUID> {

    Optional<BusinessBookingSettingsJpaEntity> findByBusinessId(UUID businessId);
}

package io.resrv.timeslot.adapter.out.persistence.settings;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsCommandPort;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BusinessBookingSettingsPersistenceAdapter
        implements BusinessBookingSettingsCommandPort, BusinessBookingSettingsQueryPort {

    private final BusinessBookingSettingsJpaRepository repository;

    BusinessBookingSettingsPersistenceAdapter(
            final BusinessBookingSettingsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final BusinessBookingSettings settings) {
        repository.save(BusinessBookingSettingsJpaEntity.fromDomain(settings));
    }

    @Override
    public Optional<BusinessBookingSettings> findByBusinessId(final BusinessId businessId) {
        return repository
                .findByBusinessId(businessId.value())
                .map(BusinessBookingSettingsJpaEntity::toDomain);
    }
}

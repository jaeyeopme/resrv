package io.resrv.timeslot.application.settings.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.domain.settings.BusinessBookingSettings;
import java.util.Optional;

public interface BusinessBookingSettingsQueryPort {

    Optional<BusinessBookingSettings> findByBusinessId(BusinessId businessId);
}

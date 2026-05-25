package io.resrv.timeslot.application.settings.out;

import io.resrv.timeslot.domain.settings.BusinessBookingSettings;

public interface BusinessBookingSettingsCommandPort {

    void save(BusinessBookingSettings settings);
}

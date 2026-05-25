package io.resrv.timeslot.application.settings.in;

public interface UpsertBusinessBookingSettingsUseCase {

    BusinessBookingSettingsResult upsert(UpsertBusinessBookingSettingsCommand command);
}

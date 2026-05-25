package io.resrv.timeslot.application.settings;

import io.resrv.shared.kernel.BusinessId;

public final class BookingSettingsRequiredException extends RuntimeException {

    public BookingSettingsRequiredException(final BusinessId businessId) {
        super("Booking settings are required for business: " + businessId.value());
    }
}

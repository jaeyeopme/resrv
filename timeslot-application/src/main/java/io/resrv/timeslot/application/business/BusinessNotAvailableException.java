package io.resrv.timeslot.application.business;

import io.resrv.shared.kernel.BusinessId;

public class BusinessNotAvailableException extends RuntimeException {

    public BusinessNotAvailableException(final BusinessId businessId) {
        super("Business is not available: " + businessId.value());
    }
}

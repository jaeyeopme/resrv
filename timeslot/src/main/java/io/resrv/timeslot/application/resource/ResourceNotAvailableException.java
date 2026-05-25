package io.resrv.timeslot.application.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;

public class ResourceNotAvailableException extends RuntimeException {

    private final BusinessId businessId;
    private final ResourceId resourceId;

    public ResourceNotAvailableException(final BusinessId businessId, final ResourceId resourceId) {
        super(
                "Resource is not available for business "
                        + businessId.value()
                        + ": "
                        + resourceId.value());
        this.businessId = businessId;
        this.resourceId = resourceId;
    }

    public BusinessId businessId() {
        return businessId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }
}

package io.resrv.timeslot.application.resource.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.util.Objects;

public record ActivateResourceCommand(BusinessId businessId, ResourceId resourceId) {

    public ActivateResourceCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
    }
}

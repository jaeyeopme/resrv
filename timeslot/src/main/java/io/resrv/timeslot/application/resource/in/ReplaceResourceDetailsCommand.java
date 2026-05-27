package io.resrv.timeslot.application.resource.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.util.Objects;

public record ReplaceResourceDetailsCommand(
        BusinessId businessId,
        ResourceId resourceId,
        String name,
        String slug,
        String description,
        Integer slotDurationMinutes,
        Integer holdTtlMinutes,
        Integer cancellationWindowMinutes) {

    public ReplaceResourceDetailsCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
    }
}

package io.resrv.timeslot.application.resource.in;

import io.resrv.shared.kernel.BusinessId;
import java.util.Objects;

public record CreateResourceCommand(
        BusinessId businessId,
        String name,
        String slug,
        String description,
        Integer slotDurationMinutes,
        Integer holdTtlMinutes,
        Integer cancellationWindowMinutes) {

    public CreateResourceCommand {
        Objects.requireNonNull(businessId, "Business id must not be null");
    }
}

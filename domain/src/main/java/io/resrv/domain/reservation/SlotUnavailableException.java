package io.resrv.domain.reservation;

import io.resrv.domain.resource.ResourceId;
import java.time.Instant;

public final class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(final ResourceId resourceId, final Instant startAt) {
        super(
                "Slot starting at '%s' is unavailable for resource '%s'"
                        .formatted(startAt, resourceId.value()));
    }
}

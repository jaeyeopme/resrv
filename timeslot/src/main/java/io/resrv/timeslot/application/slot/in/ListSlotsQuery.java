package io.resrv.timeslot.application.slot.in;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import java.time.LocalDate;
import java.util.Objects;

public record ListSlotsQuery(BusinessId businessId, ResourceId resourceId, LocalDate date) {

    public ListSlotsQuery {
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(date, "Date must not be null");
    }
}

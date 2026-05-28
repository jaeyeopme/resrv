package io.resrv.timeslot.application.discovery.in;

import io.resrv.shared.kernel.ResourceId;
import java.time.LocalDate;
import java.util.Objects;

public record PublicSlotDiscoveryQuery(String businessSlug, ResourceId resourceId, LocalDate date) {

    public PublicSlotDiscoveryQuery {
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(date, "Date must not be null");
    }
}

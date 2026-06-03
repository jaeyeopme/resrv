package io.resrv.timeslot.application.discovery.in;

import java.util.Objects;
import java.util.UUID;

public record PublicResourceDiscoveryResult(
        UUID resourceId, String businessSlug, String name, String description) {

    public PublicResourceDiscoveryResult {
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
        Objects.requireNonNull(name, "Resource name must not be null");
    }
}

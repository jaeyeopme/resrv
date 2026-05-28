package io.resrv.timeslot.application.discovery.in;

import java.util.Objects;

public record PublicResourceDiscoveryQuery(String businessSlug) {

    public PublicResourceDiscoveryQuery {
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
    }
}

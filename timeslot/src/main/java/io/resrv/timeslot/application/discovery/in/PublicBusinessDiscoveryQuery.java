package io.resrv.timeslot.application.discovery.in;

import java.util.Objects;

public record PublicBusinessDiscoveryQuery(String businessSlug) {

    public PublicBusinessDiscoveryQuery {
        Objects.requireNonNull(businessSlug, "Business slug must not be null");
    }
}

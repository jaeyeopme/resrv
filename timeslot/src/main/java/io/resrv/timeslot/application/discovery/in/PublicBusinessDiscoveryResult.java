package io.resrv.timeslot.application.discovery.in;

import java.util.Objects;

public record PublicBusinessDiscoveryResult(String slug, String name, String timezone) {

    public PublicBusinessDiscoveryResult {
        Objects.requireNonNull(slug, "Business slug must not be null");
        Objects.requireNonNull(name, "Business name must not be null");
        Objects.requireNonNull(timezone, "Business timezone must not be null");
    }
}

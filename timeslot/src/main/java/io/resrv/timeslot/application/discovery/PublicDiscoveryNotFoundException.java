package io.resrv.timeslot.application.discovery;

public final class PublicDiscoveryNotFoundException extends RuntimeException {

    public PublicDiscoveryNotFoundException() {
        super("No public bookable representation found");
    }
}

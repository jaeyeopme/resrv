package io.resrv.timeslot.application.discovery.in;

import java.util.List;

public interface PublicResourceDiscoveryUseCase {

    List<PublicResourceDiscoveryResult> listResources(PublicResourceDiscoveryQuery query);
}

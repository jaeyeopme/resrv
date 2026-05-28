package io.resrv.timeslot.application.discovery.in;

import java.util.List;

public interface PublicSlotDiscoveryUseCase {

    List<PublicSlotDiscoveryResult> listSlots(PublicSlotDiscoveryQuery query);
}

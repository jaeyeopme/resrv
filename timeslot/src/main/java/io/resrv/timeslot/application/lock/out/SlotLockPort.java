package io.resrv.timeslot.application.lock.out;

import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;

public interface SlotLockPort {

    void lockSlot(ResourceId resourceId, Instant slotStartAt);
}

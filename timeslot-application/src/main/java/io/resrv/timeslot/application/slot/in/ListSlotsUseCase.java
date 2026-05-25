package io.resrv.timeslot.application.slot.in;

import java.util.List;

public interface ListSlotsUseCase {

    List<SlotResult> listSlots(ListSlotsQuery query);
}

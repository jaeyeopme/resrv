package io.resrv.application.reservation.in;

import java.util.List;

public interface ListAvailableSlotsUseCase {

    List<SlotResult> listAvailableSlots(ListAvailableSlotsQuery query);
}

package io.resrv.ticketing.application.inventory.out;

import io.resrv.ticketing.domain.inventory.TicketInventory;

public interface TicketInventoryCommandPort {

    void save(TicketInventory inventory);
}

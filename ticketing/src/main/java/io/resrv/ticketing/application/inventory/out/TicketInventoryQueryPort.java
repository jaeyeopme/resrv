package io.resrv.ticketing.application.inventory.out;

import io.resrv.ticketing.domain.inventory.TicketInventory;
import io.resrv.ticketing.domain.inventory.TicketInventoryId;
import java.util.Optional;

public interface TicketInventoryQueryPort {

    Optional<TicketInventory> findById(TicketInventoryId ticketInventoryId);
}

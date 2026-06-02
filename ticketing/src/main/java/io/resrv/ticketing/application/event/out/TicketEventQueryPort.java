package io.resrv.ticketing.application.event.out;

import io.resrv.ticketing.domain.event.TicketEvent;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.Optional;

public interface TicketEventQueryPort {

    Optional<TicketEvent> findById(TicketEventId ticketEventId);
}

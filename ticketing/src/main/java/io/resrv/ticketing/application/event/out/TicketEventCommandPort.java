package io.resrv.ticketing.application.event.out;

import io.resrv.ticketing.domain.event.TicketEvent;

public interface TicketEventCommandPort {

    void save(TicketEvent event);
}

package io.resrv.ticketing.application.seat.out;

import io.resrv.ticketing.domain.event.TicketEventId;
import io.resrv.ticketing.domain.seat.TicketSeat;
import io.resrv.ticketing.domain.seat.TicketSeatId;
import java.util.List;
import java.util.Optional;

public interface TicketSeatQueryPort {

    Optional<TicketSeat> findById(TicketSeatId ticketSeatId);

    List<TicketSeat> findAllByIds(List<TicketSeatId> ticketSeatIds);

    List<TicketSeat> findAllByEventId(TicketEventId ticketEventId);
}

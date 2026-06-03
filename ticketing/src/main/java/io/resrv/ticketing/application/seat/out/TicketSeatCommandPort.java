package io.resrv.ticketing.application.seat.out;

import io.resrv.ticketing.domain.seat.TicketSeat;
import java.util.List;

public interface TicketSeatCommandPort {

    void save(TicketSeat seat);

    void saveAll(List<TicketSeat> seats);
}

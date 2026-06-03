package io.resrv.ticketing.application.seat.out;

import io.resrv.ticketing.domain.purchase.TicketPurchase;

public interface TicketSeatClaimPort {

    boolean claimAvailableSeats(TicketPurchase purchase);
}

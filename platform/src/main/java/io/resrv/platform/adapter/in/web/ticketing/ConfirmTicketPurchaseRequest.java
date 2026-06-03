package io.resrv.platform.adapter.in.web.ticketing;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

record ConfirmTicketPurchaseRequest(
        @NotEmpty(message = "At least one ticket seat is required") List<UUID> seatIds) {}

package io.resrv.platform.adapter.in.web.ticketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

record ConfirmTicketPurchaseRequest(
        @NotEmpty(message = "At least one ticket seat is required") List<UUID> seatIds,
        @NotBlank(message = "Idempotency key is required")
                @Size(max = 120, message = "Idempotency key must be at most 120 characters")
                String idempotencyKey) {}

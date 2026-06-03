package io.resrv.platform.adapter.in.web.ticketing;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

record ConfirmTicketPurchaseRequest(
        @ArraySchema(
                        minItems = 1,
                        schema = @Schema(format = "uuid"),
                        arraySchema =
                                @Schema(
                                        description = "Selected ticket seat IDs to purchase",
                                        requiredMode = Schema.RequiredMode.REQUIRED))
                @NotEmpty(message = "At least one ticket seat is required")
                @Size(min = 1, message = "At least one ticket seat is required")
                List<UUID> seatIds,
        @NotBlank(message = "Idempotency key is required")
                @Size(max = 120, message = "Idempotency key must be at most 120 characters")
                @Schema(
                        description = "Client-generated purchase confirmation idempotency key",
                        maxLength = 120,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String idempotencyKey) {}

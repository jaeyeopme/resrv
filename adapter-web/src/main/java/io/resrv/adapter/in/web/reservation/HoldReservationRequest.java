package io.resrv.adapter.in.web.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Payload for placing a temporary reservation hold.")
record HoldReservationRequest(
        @Schema(
                        description = "Resource identifier.",
                        example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                @NotNull
                UUID resourceId,
        @Schema(description = "Requested slot start instant.", example = "2026-05-11T01:00:00Z")
                @NotNull
                Instant startAt) {}

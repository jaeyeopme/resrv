package io.resrv.adapter.in.web.availability;

import io.resrv.application.reservation.in.SlotResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Bookable reservation slot.")
record SlotResponse(
        @Schema(description = "Slot start instant.", example = "2026-05-11T01:00:00Z")
                Instant startAt,
        @Schema(description = "Slot end instant.", example = "2026-05-11T02:00:00Z")
                Instant endAt) {

    static SlotResponse from(final SlotResult result) {
        return new SlotResponse(result.startAt(), result.endAt());
    }
}

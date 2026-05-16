package io.resrv.adapter.in.web.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "Recurring weekly availability window.")
record WeeklyAvailabilityRequest(
        @Schema(description = "Tenant-local opening time.", example = "09:00") @NotNull
                LocalTime startTime,
        @Schema(description = "Tenant-local closing time.", example = "18:00") @NotNull
                LocalTime endTime) {}

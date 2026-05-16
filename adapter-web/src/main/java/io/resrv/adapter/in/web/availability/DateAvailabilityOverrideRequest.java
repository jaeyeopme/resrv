package io.resrv.adapter.in.web.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Date-specific availability override.")
record DateAvailabilityOverrideRequest(
        @Schema(
                        description = "Whether the resource is closed for the whole date.",
                        example = "false")
                boolean closed,
        @Schema(
                        description = "Tenant-local override start time when not closed.",
                        example = "10:00")
                LocalTime startTime,
        @Schema(description = "Tenant-local override end time when not closed.", example = "16:00")
                LocalTime endTime) {}

package io.resrv.timeslot.adapter.in.web.slot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface SlotApiDocs {

    @Operation(
            summary = "List public bookable slots",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Bookable slots returned for active business/resource state"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Business settings or resource not available"),
                @ApiResponse(responseCode = "422", description = "Booking settings are required")
            })
    List<SlotWebAdapter.SlotResponse> list(UUID businessId, UUID resourceId, LocalDate date);
}

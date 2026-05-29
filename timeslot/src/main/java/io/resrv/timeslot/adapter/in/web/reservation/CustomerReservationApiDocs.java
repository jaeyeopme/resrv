package io.resrv.timeslot.adapter.in.web.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

interface CustomerReservationApiDocs {

    @Operation(
            summary = "List my reservations",
            responses = {
                @ApiResponse(responseCode = "200", description = "Customer reservations returned"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required")
            })
    CustomerReservationWebAdapter.CustomerReservationPageResponse list(
            JwtAuthenticationToken authentication,
            @Min(0) int page,
            @Min(1) @Max(100) int size,
            String state,
            Boolean upcoming);

    @Operation(
            summary = "Get my reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Customer reservation returned"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found")
            })
    CustomerReservationWebAdapter.CustomerReservationResponse detail(
            JwtAuthenticationToken authentication, UUID reservationId);
}

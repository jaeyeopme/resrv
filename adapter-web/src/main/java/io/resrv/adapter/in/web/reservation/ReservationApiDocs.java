package io.resrv.adapter.in.web.reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Tag(
        name = "Reservations",
        description = "Customer reservation hold, confirmation, listing, and cancellation")
@SecurityRequirement(name = "bearerAuth")
interface ReservationApiDocs {

    @Operation(
            summary = "Hold a reservation slot",
            description =
                    "Creates a temporary customer hold for a resource start time. "
                            + "The authenticated JWT must represent a customer.")
    @ApiResponse(responseCode = "201", description = "Reservation hold created")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid reservation hold payload",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not a customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Slot is unavailable or conflicts with another reservation",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ReservationResponse> hold(
            HoldReservationRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Confirm a held reservation",
            description = "Confirms an existing customer hold before it expires.")
    @ApiResponse(responseCode = "200", description = "Reservation confirmed")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not the reservation customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Reservation not found for the authenticated customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Reservation cannot transition to confirmed",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ReservationResponse> confirm(
            @Parameter(
                            description = "Reservation identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID reservationId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "List my reservations",
            description = "Returns reservations owned by the authenticated customer.")
    @ApiResponse(responseCode = "200", description = "Customer reservations returned")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not a customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<List<ReservationResponse>> listMine(
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Cancel my reservation",
            description = "Cancels one reservation owned by the authenticated customer.")
    @ApiResponse(responseCode = "200", description = "Reservation cancelled")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not the reservation customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Reservation not found for the authenticated customer",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Reservation cannot transition to cancelled",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ReservationResponse> cancel(
            @Parameter(
                            description = "Reservation identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID reservationId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);
}

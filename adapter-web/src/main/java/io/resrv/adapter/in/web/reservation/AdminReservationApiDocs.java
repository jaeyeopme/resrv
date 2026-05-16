package io.resrv.adapter.in.web.reservation;

import io.resrv.domain.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Tag(name = "Admin Reservations", description = "Internal reservation operations for tenant admins")
@SecurityRequirement(name = "bearerAuth")
interface AdminReservationApiDocs {

    @Operation(
            summary = "Search reservations as an administrator",
            description =
                    "Lists reservations for the authenticated tenant and tenant-local date. "
                            + "Optional resource, customer, and status filters can be combined.")
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    @ApiResponse(
            responseCode = "400",
            description = "Missing or invalid date/filter parameter",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Filter resource or customer not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<List<ReservationResponse>> list(
            @Parameter(description = "Tenant-local date to search.", example = "2026-05-11")
                    LocalDate date,
            @Parameter(
                            description = "Optional resource identifier filter.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(
                            description = "Optional customer identifier filter.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    UUID customerId,
            @Parameter(description = "Optional reservation status filter.", example = "CONFIRMED")
                    ReservationStatus status,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Cancel a reservation as an administrator",
            description = "Marks a held or confirmed reservation as ADMIN_CANCELLED.")
    @ApiResponse(responseCode = "200", description = "Reservation cancelled")
    @AdminReservationErrorResponses
    ResponseEntity<ReservationResponse> adminCancel(
            @Parameter(description = "Reservation identifier.") UUID reservationId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Check in a reservation as an administrator",
            description = "Marks a confirmed reservation as CHECKED_IN at or after its start time.")
    @ApiResponse(responseCode = "200", description = "Reservation checked in")
    @AdminReservationErrorResponses
    ResponseEntity<ReservationResponse> checkIn(
            @Parameter(description = "Reservation identifier.") UUID reservationId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Mark a reservation no-show as an administrator",
            description = "Marks a confirmed reservation as NO_SHOW at or after its end time.")
    @ApiResponse(responseCode = "200", description = "Reservation marked no-show")
    @AdminReservationErrorResponses
    ResponseEntity<ReservationResponse> markNoShow(
            @Parameter(description = "Reservation identifier.") UUID reservationId,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);
}

package io.resrv.adapter.in.web.availability;

import io.resrv.adapter.in.web.reservation.ReservationResponse;
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

@Tag(
        name = "Availability",
        description = "Resource availability, slot search, and admin reservation audit")
@SecurityRequirement(name = "bearerAuth")
interface AvailabilityApiDocs {

    @Operation(
            summary = "Upsert weekly availability",
            description =
                    "Creates or updates recurring bookable hours for a resource and day of week.")
    @ApiResponse(responseCode = "200", description = "Weekly availability saved")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid day of week, resource id, or time range",
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
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<WeeklyAvailabilityResponse> upsertWeekly(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(
                            description = "Java DayOfWeek value: 1 Monday through 7 Sunday.",
                            example = "1")
                    int dayOfWeek,
            WeeklyAvailabilityRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Delete weekly availability",
            description = "Removes recurring bookable hours for a resource and day of week.")
    @ApiResponse(responseCode = "204", description = "Weekly availability deleted")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid day of week or resource id",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deleteWeekly(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(
                            description = "Java DayOfWeek value: 1 Monday through 7 Sunday.",
                            example = "1")
                    int dayOfWeek,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Upsert date availability exception",
            description = "Creates or updates a date-specific closure or special-hours override.")
    @ApiResponse(responseCode = "200", description = "Date availability exception saved")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid date, resource id, or time range",
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
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<DateAvailabilityOverrideResponse> upsertDateOverride(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(
                            description = "Exception date in tenant-local calendar.",
                            example = "2026-05-11")
                    LocalDate date,
            DateAvailabilityOverrideRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "Delete date availability exception",
            description = "Removes a date-specific closure or special-hours override.")
    @ApiResponse(responseCode = "204", description = "Date availability exception deleted")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid date or resource id",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Authenticated principal is not an administrator",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deleteDateOverride(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(
                            description = "Exception date in tenant-local calendar.",
                            example = "2026-05-11")
                    LocalDate date,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "List available slots",
            description = "Calculates available slots for a resource and tenant-local date.")
    @ApiResponse(responseCode = "200", description = "Available slots returned")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid date or resource id",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid Bearer token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<List<SlotResponse>> listSlots(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(description = "Tenant-local date to search.", example = "2026-05-11")
                    LocalDate date,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);

    @Operation(
            summary = "List resource reservations",
            description =
                    "Lists reservations for one resource and tenant-local date for administrator audit.")
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid date or resource id",
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
            description = "Resource not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<List<ReservationResponse>> listResourceReservations(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID resourceId,
            @Parameter(description = "Tenant-local date to audit.", example = "2026-05-11")
                    LocalDate date,
            @Parameter(hidden = true) JwtAuthenticationToken authentication);
}

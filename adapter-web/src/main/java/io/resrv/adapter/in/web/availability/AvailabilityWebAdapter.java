package io.resrv.adapter.in.web.availability;

import io.resrv.adapter.in.web.security.AuthenticatedPrincipal;
import io.resrv.application.availability.in.DateAvailabilityOverrideResult;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.WeeklyAvailabilityResult;
import io.resrv.application.reservation.in.ListAvailableSlotsQuery;
import io.resrv.application.reservation.in.ListAvailableSlotsUseCase;
import io.resrv.application.reservation.in.ListResourceReservationsQuery;
import io.resrv.application.reservation.in.ListResourceReservationsUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.application.reservation.in.SlotResult;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources/{resourceId}")
@Tag(
        name = "Availability",
        description = "Resource availability, slot search, and admin reservation audit")
@SecurityRequirement(name = "bearerAuth")
class AvailabilityWebAdapter {

    private final UpsertWeeklyAvailabilityUseCase upsertWeeklyAvailabilityUseCase;
    private final DeleteWeeklyAvailabilityUseCase deleteWeeklyAvailabilityUseCase;
    private final UpsertDateAvailabilityOverrideUseCase upsertDateAvailabilityOverrideUseCase;
    private final DeleteDateAvailabilityOverrideUseCase deleteDateAvailabilityOverrideUseCase;
    private final ListAvailableSlotsUseCase listAvailableSlotsUseCase;
    private final ListResourceReservationsUseCase listResourceReservationsUseCase;

    AvailabilityWebAdapter(
            final UpsertWeeklyAvailabilityUseCase upsertWeeklyAvailabilityUseCase,
            final DeleteWeeklyAvailabilityUseCase deleteWeeklyAvailabilityUseCase,
            final UpsertDateAvailabilityOverrideUseCase upsertDateAvailabilityOverrideUseCase,
            final DeleteDateAvailabilityOverrideUseCase deleteDateAvailabilityOverrideUseCase,
            final ListAvailableSlotsUseCase listAvailableSlotsUseCase,
            final ListResourceReservationsUseCase listResourceReservationsUseCase) {
        this.upsertWeeklyAvailabilityUseCase = upsertWeeklyAvailabilityUseCase;
        this.deleteWeeklyAvailabilityUseCase = deleteWeeklyAvailabilityUseCase;
        this.upsertDateAvailabilityOverrideUseCase = upsertDateAvailabilityOverrideUseCase;
        this.deleteDateAvailabilityOverrideUseCase = deleteDateAvailabilityOverrideUseCase;
        this.listAvailableSlotsUseCase = listAvailableSlotsUseCase;
        this.listResourceReservationsUseCase = listResourceReservationsUseCase;
    }

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
    @PutMapping("/weekly-availability/{dayOfWeek}")
    ResponseEntity<WeeklyAvailabilityResponse> upsertWeekly(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(
                            description = "Java DayOfWeek value: 1 Monday through 7 Sunday.",
                            example = "1")
                    @PathVariable
                    final int dayOfWeek,
            @Valid @RequestBody final WeeklyAvailabilityRequest request,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                WeeklyAvailabilityResponse.from(
                        upsertWeeklyAvailabilityUseCase.upsert(
                                new UpsertWeeklyAvailabilityCommand(
                                        principal.tenantId(),
                                        ResourceId.of(resourceId),
                                        DayOfWeek.of(dayOfWeek),
                                        request.startTime(),
                                        request.endTime())));
        return ResponseEntity.ok(response);
    }

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
    @DeleteMapping("/weekly-availability/{dayOfWeek}")
    ResponseEntity<Void> deleteWeekly(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(
                            description = "Java DayOfWeek value: 1 Monday through 7 Sunday.",
                            example = "1")
                    @PathVariable
                    final int dayOfWeek,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteWeeklyAvailabilityUseCase.delete(
                new DeleteWeeklyAvailabilityCommand(
                        principal.tenantId(), ResourceId.of(resourceId), DayOfWeek.of(dayOfWeek)));
        return ResponseEntity.noContent().build();
    }

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
    @PutMapping("/availability-exceptions/{date}")
    ResponseEntity<DateAvailabilityOverrideResponse> upsertDateOverride(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(
                            description = "Exception date in tenant-local calendar.",
                            example = "2026-05-11")
                    @PathVariable
                    final LocalDate date,
            @Valid @RequestBody final DateAvailabilityOverrideRequest request,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                DateAvailabilityOverrideResponse.from(
                        upsertDateAvailabilityOverrideUseCase.upsert(
                                new UpsertDateAvailabilityOverrideCommand(
                                        principal.tenantId(),
                                        ResourceId.of(resourceId),
                                        date,
                                        request.closed(),
                                        request.startTime(),
                                        request.endTime())));
        return ResponseEntity.ok(response);
    }

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
    @DeleteMapping("/availability-exceptions/{date}")
    ResponseEntity<Void> deleteDateOverride(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(
                            description = "Exception date in tenant-local calendar.",
                            example = "2026-05-11")
                    @PathVariable
                    final LocalDate date,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteDateAvailabilityOverrideUseCase.delete(
                new DeleteDateAvailabilityOverrideCommand(
                        principal.tenantId(), ResourceId.of(resourceId), date));
        return ResponseEntity.noContent().build();
    }

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
    @GetMapping("/slots")
    ResponseEntity<List<SlotResponse>> listSlots(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(description = "Tenant-local date to search.", example = "2026-05-11")
                    @RequestParam
                    final LocalDate date,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication);
        final var response =
                listAvailableSlotsUseCase
                        .listAvailableSlots(
                                new ListAvailableSlotsQuery(
                                        principal.tenantId(), ResourceId.of(resourceId), date))
                        .stream()
                        .map(SlotResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

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
    @GetMapping("/reservations")
    ResponseEntity<List<ReservationResponse>> listResourceReservations(
            @Parameter(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID resourceId,
            @Parameter(description = "Tenant-local date to audit.", example = "2026-05-11")
                    @RequestParam
                    final LocalDate date,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                listResourceReservationsUseCase
                        .listResourceReservations(
                                new ListResourceReservationsQuery(
                                        principal.tenantId(), ResourceId.of(resourceId), date))
                        .stream()
                        .map(ReservationResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

    @Schema(description = "Recurring weekly availability window.")
    record WeeklyAvailabilityRequest(
            @Schema(description = "Tenant-local opening time.", example = "09:00") @NotNull
                    LocalTime startTime,
            @Schema(description = "Tenant-local closing time.", example = "18:00") @NotNull
                    LocalTime endTime) {}

    @Schema(description = "Date-specific availability override.")
    record DateAvailabilityOverrideRequest(
            @Schema(
                            description = "Whether the resource is closed for the whole date.",
                            example = "false")
                    boolean closed,
            @Schema(
                            description = "Tenant-local override start time when not closed.",
                            example = "10:00")
                    @Nullable LocalTime startTime,
            @Schema(
                            description = "Tenant-local override end time when not closed.",
                            example = "16:00")
                    @Nullable LocalTime endTime) {}

    @Schema(description = "Saved recurring weekly availability window.")
    record WeeklyAvailabilityResponse(
            @Schema(
                            description = "Weekly availability identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID id,
            @Schema(
                            description = "Tenant identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    UUID tenantId,
            @Schema(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                    UUID resourceId,
            @Schema(description = "Day of week for the recurring window.", example = "MONDAY")
                    DayOfWeek dayOfWeek,
            @Schema(description = "Tenant-local opening time.", example = "09:00")
                    LocalTime startTime,
            @Schema(description = "Tenant-local closing time.", example = "18:00")
                    LocalTime endTime,
            @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant createdAt,
            @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant updatedAt) {

        static WeeklyAvailabilityResponse from(final WeeklyAvailabilityResult result) {
            return new WeeklyAvailabilityResponse(
                    result.id(),
                    result.tenantId(),
                    result.resourceId(),
                    result.dayOfWeek(),
                    result.startTime(),
                    result.endTime(),
                    result.createdAt(),
                    result.updatedAt());
        }
    }

    @Schema(description = "Saved date-specific availability override.")
    record DateAvailabilityOverrideResponse(
            @Schema(
                            description = "Date override identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID id,
            @Schema(
                            description = "Tenant identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    UUID tenantId,
            @Schema(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                    UUID resourceId,
            @Schema(description = "Tenant-local override date.", example = "2026-05-11")
                    LocalDate date,
            @Schema(
                            description = "Whether the resource is closed for the whole date.",
                            example = "false")
                    boolean closed,
            @Schema(
                            description = "Tenant-local override start time when not closed.",
                            example = "10:00")
                    @Nullable LocalTime startTime,
            @Schema(
                            description = "Tenant-local override end time when not closed.",
                            example = "16:00")
                    @Nullable LocalTime endTime,
            @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant createdAt,
            @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant updatedAt) {

        static DateAvailabilityOverrideResponse from(final DateAvailabilityOverrideResult result) {
            return new DateAvailabilityOverrideResponse(
                    result.id(),
                    result.tenantId(),
                    result.resourceId(),
                    result.date(),
                    result.closed(),
                    result.startTime(),
                    result.endTime(),
                    result.createdAt(),
                    result.updatedAt());
        }
    }

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

    @Schema(description = "Reservation visible to an administrator.")
    record ReservationResponse(
            @Schema(
                            description = "Reservation identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    UUID id,
            @Schema(
                            description = "Tenant identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    UUID tenantId,
            @Schema(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                    UUID resourceId,
            @Schema(
                            description = "Customer identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91f")
                    UUID customerId,
            @Schema(description = "Reservation start instant.", example = "2026-05-11T01:00:00Z")
                    Instant startAt,
            @Schema(description = "Reservation end instant.", example = "2026-05-11T02:00:00Z")
                    Instant endAt,
            @Schema(description = "Reservation lifecycle state.", example = "HELD")
                    ReservationStatus status,
            @Schema(
                            description = "Hold expiration timestamp for unconfirmed holds.",
                            example = "2026-05-11T00:10:00Z")
                    @Nullable Instant holdExpiresAt,
            @Schema(description = "Creation timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant createdAt,
            @Schema(description = "Last update timestamp.", example = "2026-05-10T00:00:00Z")
                    Instant updatedAt,
            @Schema(
                            description = "Confirmation timestamp, if confirmed.",
                            example = "2026-05-10T00:01:00Z")
                    @Nullable Instant confirmedAt,
            @Schema(
                            description = "Cancellation timestamp, if cancelled.",
                            example = "2026-05-10T00:02:00Z")
                    @Nullable Instant cancelledAt) {

        static ReservationResponse from(final ReservationResult result) {
            return new ReservationResponse(
                    result.id(),
                    result.tenantId(),
                    result.resourceId(),
                    result.customerId(),
                    result.startAt(),
                    result.endAt(),
                    result.status(),
                    result.holdExpiresAt(),
                    result.createdAt(),
                    result.updatedAt(),
                    result.confirmedAt(),
                    result.cancelledAt());
        }
    }
}

package io.resrv.adapter.in.web.reservation;

import io.resrv.adapter.in.web.security.AuthenticatedPrincipal;
import io.resrv.application.reservation.in.AdminCancelReservationCommand;
import io.resrv.application.reservation.in.AdminCancelReservationUseCase;
import io.resrv.application.reservation.in.CheckInReservationCommand;
import io.resrv.application.reservation.in.CheckInReservationUseCase;
import io.resrv.application.reservation.in.ListAdminReservationsQuery;
import io.resrv.application.reservation.in.ListAdminReservationsUseCase;
import io.resrv.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.application.reservation.in.MarkNoShowReservationUseCase;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin Reservations", description = "Internal reservation operations for tenant admins")
@SecurityRequirement(name = "bearerAuth")
class AdminReservationWebAdapter {

    private final ListAdminReservationsUseCase listAdminReservationsUseCase;
    private final AdminCancelReservationUseCase adminCancelReservationUseCase;
    private final CheckInReservationUseCase checkInReservationUseCase;
    private final MarkNoShowReservationUseCase markNoShowReservationUseCase;

    AdminReservationWebAdapter(
            final ListAdminReservationsUseCase listAdminReservationsUseCase,
            final AdminCancelReservationUseCase adminCancelReservationUseCase,
            final CheckInReservationUseCase checkInReservationUseCase,
            final MarkNoShowReservationUseCase markNoShowReservationUseCase) {
        this.listAdminReservationsUseCase = listAdminReservationsUseCase;
        this.adminCancelReservationUseCase = adminCancelReservationUseCase;
        this.checkInReservationUseCase = checkInReservationUseCase;
        this.markNoShowReservationUseCase = markNoShowReservationUseCase;
    }

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
    @GetMapping("/api/reservations")
    ResponseEntity<List<ReservationResponse>> list(
            @Parameter(description = "Tenant-local date to search.", example = "2026-05-11")
                    @RequestParam
                    final LocalDate date,
            @Parameter(
                            description = "Optional resource identifier filter.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @RequestParam(required = false)
                    final UUID resourceId,
            @Parameter(
                            description = "Optional customer identifier filter.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91d")
                    @RequestParam(required = false)
                    final UUID customerId,
            @Parameter(description = "Optional reservation status filter.", example = "CONFIRMED")
                    @RequestParam(required = false)
                    final ReservationStatus status,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                listAdminReservationsUseCase
                        .listAdminReservations(
                                new ListAdminReservationsQuery(
                                        principal.tenantId(),
                                        date,
                                        Optional.ofNullable(resourceId).map(ResourceId::of),
                                        Optional.ofNullable(customerId).map(CustomerId::of),
                                        Optional.ofNullable(status)))
                        .stream()
                        .map(ReservationResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel a reservation as an administrator",
            description = "Marks a held or confirmed reservation as ADMIN_CANCELLED.")
    @ApiResponse(responseCode = "200", description = "Reservation cancelled")
    @AdminReservationErrorResponses
    @PostMapping("/api/reservations/{reservationId}/admin-cancel")
    ResponseEntity<ReservationResponse> adminCancel(
            @Parameter(description = "Reservation identifier.") @PathVariable
                    final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        adminCancelReservationUseCase.adminCancel(
                                new AdminCancelReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Check in a reservation as an administrator",
            description = "Marks a confirmed reservation as CHECKED_IN at or after its start time.")
    @ApiResponse(responseCode = "200", description = "Reservation checked in")
    @AdminReservationErrorResponses
    @PostMapping("/api/reservations/{reservationId}/check-in")
    ResponseEntity<ReservationResponse> checkIn(
            @Parameter(description = "Reservation identifier.") @PathVariable
                    final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        checkInReservationUseCase.checkIn(
                                new CheckInReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark a reservation no-show as an administrator",
            description = "Marks a confirmed reservation as NO_SHOW at or after its end time.")
    @ApiResponse(responseCode = "200", description = "Reservation marked no-show")
    @AdminReservationErrorResponses
    @PostMapping("/api/reservations/{reservationId}/no-show")
    ResponseEntity<ReservationResponse> markNoShow(
            @Parameter(description = "Reservation identifier.") @PathVariable
                    final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        markNoShowReservationUseCase.markNoShow(
                                new MarkNoShowReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @ApiResponse(
            responseCode = "400",
            description = "Invalid reservation id",
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
            description = "Reservation not found in the authenticated tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Reservation cannot transition from its current state",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AdminReservationErrorResponses {}
}

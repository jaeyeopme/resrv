package io.resrv.adapter.in.web.reservation;

import io.resrv.adapter.in.web.security.AuthenticatedPrincipal;
import io.resrv.application.reservation.in.CancelCustomerReservationCommand;
import io.resrv.application.reservation.in.CancelCustomerReservationUseCase;
import io.resrv.application.reservation.in.ConfirmReservationCommand;
import io.resrv.application.reservation.in.ConfirmReservationUseCase;
import io.resrv.application.reservation.in.HoldReservationCommand;
import io.resrv.application.reservation.in.HoldReservationUseCase;
import io.resrv.application.reservation.in.ListCustomerReservationsQuery;
import io.resrv.application.reservation.in.ListCustomerReservationsUseCase;
import io.resrv.domain.reservation.ReservationId;
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
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Reservations",
        description = "Customer reservation hold, confirmation, listing, and cancellation")
@SecurityRequirement(name = "bearerAuth")
class ReservationWebAdapter {

    private final HoldReservationUseCase holdReservationUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ListCustomerReservationsUseCase listCustomerReservationsUseCase;
    private final CancelCustomerReservationUseCase cancelCustomerReservationUseCase;

    ReservationWebAdapter(
            final HoldReservationUseCase holdReservationUseCase,
            final ConfirmReservationUseCase confirmReservationUseCase,
            final ListCustomerReservationsUseCase listCustomerReservationsUseCase,
            final CancelCustomerReservationUseCase cancelCustomerReservationUseCase) {
        this.holdReservationUseCase = holdReservationUseCase;
        this.confirmReservationUseCase = confirmReservationUseCase;
        this.listCustomerReservationsUseCase = listCustomerReservationsUseCase;
        this.cancelCustomerReservationUseCase = cancelCustomerReservationUseCase;
    }

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
    @PostMapping("/api/reservation-holds")
    ResponseEntity<ReservationResponse> hold(
            @Valid @RequestBody final HoldReservationRequest request,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireCustomer();
        final var response =
                ReservationResponse.from(
                        holdReservationUseCase.hold(
                                new HoldReservationCommand(
                                        principal.tenantId(),
                                        principal.customerId(),
                                        ResourceId.of(request.resourceId()),
                                        request.startAt())));
        return ResponseEntity.created(URI.create("/api/me/reservations/" + response.id()))
                .body(response);
    }

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
    @PostMapping("/api/reservation-holds/{reservationId}/confirm")
    ResponseEntity<ReservationResponse> confirm(
            @Parameter(
                            description = "Reservation identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireCustomer();
        final var response =
                ReservationResponse.from(
                        confirmReservationUseCase.confirm(
                                new ConfirmReservationCommand(
                                        principal.tenantId(),
                                        principal.customerId(),
                                        ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

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
    @GetMapping("/api/me/reservations")
    ResponseEntity<List<ReservationResponse>> listMine(
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireCustomer();
        final var response =
                listCustomerReservationsUseCase
                        .listCustomerReservations(
                                new ListCustomerReservationsQuery(
                                        principal.tenantId(), principal.customerId()))
                        .stream()
                        .map(ReservationResponse::from)
                        .toList();
        return ResponseEntity.ok(response);
    }

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
    @PostMapping("/api/me/reservations/{reservationId}/cancel")
    ResponseEntity<ReservationResponse> cancel(
            @Parameter(
                            description = "Reservation identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91c")
                    @PathVariable
                    final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireCustomer();
        final var response =
                ReservationResponse.from(
                        cancelCustomerReservationUseCase.cancel(
                                new CancelCustomerReservationCommand(
                                        principal.tenantId(),
                                        principal.customerId(),
                                        ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @Schema(description = "Payload for placing a temporary reservation hold.")
    record HoldReservationRequest(
            @Schema(
                            description = "Resource identifier.",
                            example = "019e0cde-8f59-7832-bdeb-94d5f325f91e")
                    @NotNull
                    UUID resourceId,
            @Schema(description = "Requested slot start instant.", example = "2026-05-11T01:00:00Z")
                    @NotNull
                    Instant startAt) {}
}

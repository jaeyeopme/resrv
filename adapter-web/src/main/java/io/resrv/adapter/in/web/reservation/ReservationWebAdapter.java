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
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
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

    @PostMapping("/api/reservation-holds/{reservationId}/confirm")
    ResponseEntity<ReservationResponse> confirm(
            @PathVariable final UUID reservationId, final JwtAuthenticationToken authentication) {
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

    @PostMapping("/api/me/reservations/{reservationId}/cancel")
    ResponseEntity<ReservationResponse> cancel(
            @PathVariable final UUID reservationId, final JwtAuthenticationToken authentication) {
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

    record HoldReservationRequest(@NotNull UUID resourceId, @NotNull Instant startAt) {}

    record ReservationResponse(
            UUID id,
            UUID tenantId,
            UUID resourceId,
            UUID customerId,
            Instant startAt,
            Instant endAt,
            ReservationStatus status,
            @Nullable Instant holdExpiresAt,
            Instant createdAt,
            Instant updatedAt,
            @Nullable Instant confirmedAt,
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

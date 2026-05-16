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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminReservationWebAdapter implements AdminReservationApiDocs {

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

    @Override
    @GetMapping("/api/reservations")
    public ResponseEntity<List<ReservationResponse>> list(
            @RequestParam final LocalDate date,
            @RequestParam(required = false) final UUID resourceId,
            @RequestParam(required = false) final UUID customerId,
            @RequestParam(required = false) final ReservationStatus status,
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

    @Override
    @PostMapping("/api/reservations/{reservationId}/admin-cancel")
    public ResponseEntity<ReservationResponse> adminCancel(
            @PathVariable final UUID reservationId, final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        adminCancelReservationUseCase.adminCancel(
                                new AdminCancelReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/api/reservations/{reservationId}/check-in")
    public ResponseEntity<ReservationResponse> checkIn(
            @PathVariable final UUID reservationId, final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        checkInReservationUseCase.checkIn(
                                new CheckInReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/api/reservations/{reservationId}/no-show")
    public ResponseEntity<ReservationResponse> markNoShow(
            @PathVariable final UUID reservationId, final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        final var response =
                ReservationResponse.from(
                        markNoShowReservationUseCase.markNoShow(
                                new MarkNoShowReservationCommand(
                                        principal.tenantId(), ReservationId.of(reservationId))));
        return ResponseEntity.ok(response);
    }
}

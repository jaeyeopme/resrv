package io.resrv.timeslot.adapter.in.web.reservation;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.reservation.ReservationService;
import io.resrv.timeslot.application.reservation.in.CancelReservationCommand;
import io.resrv.timeslot.application.reservation.in.CheckInReservationCommand;
import io.resrv.timeslot.application.reservation.in.ConfirmReservationCommand;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReleaseReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReservationResult;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/reservations")
class ReservationWebAdapter {

    private final ReservationService service;
    private final BusinessLookupPort businessLookupPort;

    ReservationWebAdapter(
            final ReservationService service, final BusinessLookupPort businessLookupPort) {
        this.service = service;
        this.businessLookupPort = businessLookupPort;
    }

    @PostMapping
    ReservationResponse hold(
            @PathVariable final UUID businessId,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final HoldRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        return toResponse(
                service.hold(
                        new HoldReservationCommand(
                                BusinessId.of(businessId),
                                ResourceId.of(request.resourceId()),
                                account.accountId(),
                                request.slotId())));
    }

    @PostMapping("/{reservationId}/confirm")
    ReservationResponse confirm(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var account = AuthenticatedAccount.from(authentication);
        return toResponse(
                service.confirm(
                        new ConfirmReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId())));
    }

    @PostMapping("/{reservationId}/release")
    ReservationResponse release(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var account = AuthenticatedAccount.from(authentication);
        return toResponse(
                service.release(
                        new ReleaseReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId())));
    }

    @PostMapping("/{reservationId}/cancel")
    ReservationResponse cancel(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication,
            @RequestBody(required = false) final CancelRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        final var actor =
                request == null || request.actor() == null
                        ? ReservationCancellationActor.CUSTOMER
                        : request.actor();
        return toResponse(
                service.cancel(
                        new CancelReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId(),
                                actor)));
    }

    @PostMapping("/{reservationId}/check-in")
    ReservationResponse checkIn(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var account = AuthenticatedAccount.from(authentication);
        return toResponse(
                service.checkIn(
                        new CheckInReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId())));
    }

    @PostMapping("/{reservationId}/no-show")
    ReservationResponse noShow(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication) {
        final var account = AuthenticatedAccount.from(authentication);
        return toResponse(
                service.markNoShow(
                        new MarkNoShowReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId())));
    }

    private ReservationResponse toResponse(final ReservationResult result) {
        final var zone =
                businessLookupPort
                        .findActiveById(BusinessId.of(result.businessId()))
                        .map(business -> business.timezone().value())
                        .orElse(ZoneOffset.UTC);
        return ReservationResponse.from(result, zone);
    }

    record HoldRequest(@NotNull UUID resourceId, @NotBlank String slotId) {}

    record CancelRequest(ReservationCancellationActor actor) {}

    record ReservationResponse(
            UUID id,
            UUID businessId,
            UUID resourceId,
            UUID customerAccountId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String state,
            OffsetDateTime holdExpiresAt) {

        static ReservationResponse from(
                final ReservationResult result, final java.time.ZoneId zone) {
            return new ReservationResponse(
                    result.id(),
                    result.businessId(),
                    result.resourceId(),
                    result.customerAccountId(),
                    result.startAt().atZone(zone).toOffsetDateTime(),
                    result.endAt().atZone(zone).toOffsetDateTime(),
                    result.state().name(),
                    result.holdExpiresAt().atZone(zone).toOffsetDateTime());
        }
    }
}

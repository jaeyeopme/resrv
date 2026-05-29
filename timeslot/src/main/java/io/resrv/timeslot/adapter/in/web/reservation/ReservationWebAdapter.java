package io.resrv.timeslot.adapter.in.web.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.timeslot.application.reservation.ReservationService;
import io.resrv.timeslot.application.reservation.in.CancelReservationCommand;
import io.resrv.timeslot.application.reservation.in.CheckInReservationCommand;
import io.resrv.timeslot.application.reservation.in.ConfirmReservationCommand;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.in.ListBusinessReservationsQuery;
import io.resrv.timeslot.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReleaseReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReservationResult;
import io.resrv.timeslot.domain.reservation.ReservationCancellationActor;
import io.resrv.timeslot.domain.reservation.ReservationState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/reservations")
class ReservationWebAdapter {

    private final ReservationService service;

    ReservationWebAdapter(final ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Hold reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation hold created"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Business access is required"),
                @ApiResponse(responseCode = "404", description = "Business or resource not found"),
                @ApiResponse(responseCode = "422", description = "Slot unavailable")
            })
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

    @GetMapping
    @Operation(
            summary = "List business reservations",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservations returned"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Business access is required"),
                @ApiResponse(responseCode = "404", description = "Business not found")
            })
    List<ReservationResponse> list(
            @PathVariable final UUID businessId,
            final JwtAuthenticationToken authentication,
            @RequestParam final LocalDate date,
            @RequestParam(required = false) final UUID resourceId,
            @RequestParam(required = false) final UUID customerAccountId,
            @RequestParam(required = false) final String state) {
        final var account = AuthenticatedAccount.from(authentication);
        final var stateFilter = ReservationStateFilter.parse(state);
        final var query =
                new ListBusinessReservationsQuery(
                        BusinessId.of(businessId),
                        account.accountId(),
                        date,
                        resourceId == null ? null : ResourceId.of(resourceId),
                        customerAccountId == null ? null : AccountId.of(customerAccountId),
                        stateFilter == null ? null : stateFilter.domainState());
        final var results = service.listBusinessReservations(query);
        return results.stream().map(ReservationResponse::from).toList();
    }

    @PostMapping("/{reservationId}/confirm")
    @Operation(
            summary = "Confirm reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be confirmed")
            })
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
    @Operation(
            summary = "Release reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation released"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be released")
            })
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
    @Operation(
            summary = "Cancel reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation cancelled"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be cancelled")
            })
    ReservationResponse cancel(
            @PathVariable final UUID businessId,
            @PathVariable final UUID reservationId,
            final JwtAuthenticationToken authentication,
            @RequestBody(required = false) final CancelRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        final var actor =
                request == null || request.actor() == null
                        ? ReservationCancellationActor.CUSTOMER
                        : request.actor().domainActor();
        return toResponse(
                service.cancel(
                        new CancelReservationCommand(
                                BusinessId.of(businessId),
                                ReservationId.of(reservationId),
                                account.accountId(),
                                actor)));
    }

    @PostMapping("/{reservationId}/check-in")
    @Operation(
            summary = "Check in reservation",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation checked in"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(responseCode = "422", description = "Reservation cannot be checked in")
            })
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
    @Operation(
            summary = "Mark reservation no-show",
            responses = {
                @ApiResponse(responseCode = "200", description = "Reservation marked no-show"),
                @ApiResponse(responseCode = "400", description = "Validation failure"),
                @ApiResponse(responseCode = "401", description = "Authentication is required"),
                @ApiResponse(responseCode = "403", description = "Reservation access is required"),
                @ApiResponse(responseCode = "404", description = "Reservation not found"),
                @ApiResponse(
                        responseCode = "422",
                        description = "Reservation cannot be marked no-show")
            })
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
        return ReservationResponse.from(result);
    }

    record HoldRequest(@NotNull UUID resourceId, @NotBlank String slotId) {}

    record CancelRequest(CancelActor actor) {}

    enum CancelActor {
        CUSTOMER(ReservationCancellationActor.CUSTOMER),
        BUSINESS(ReservationCancellationActor.BUSINESS);

        private final ReservationCancellationActor domainActor;

        CancelActor(final ReservationCancellationActor domainActor) {
            this.domainActor = domainActor;
        }

        ReservationCancellationActor domainActor() {
            return domainActor;
        }
    }

    enum ReservationStateFilter {
        HELD(ReservationState.HELD),
        EXPIRED(ReservationState.EXPIRED),
        CONFIRMED(ReservationState.CONFIRMED),
        RELEASED(ReservationState.RELEASED),
        CUSTOMER_CANCELLED(ReservationState.CUSTOMER_CANCELLED),
        BUSINESS_CANCELLED(ReservationState.BUSINESS_CANCELLED),
        CHECKED_IN(ReservationState.CHECKED_IN),
        NO_SHOW(ReservationState.NO_SHOW);

        private final ReservationState domainState;

        ReservationStateFilter(final ReservationState domainState) {
            this.domainState = domainState;
        }

        ReservationState domainState() {
            return domainState;
        }

        static ReservationStateFilter parse(final String value) {
            if (value == null) {
                return null;
            }
            try {
                return ReservationStateFilter.valueOf(value);
            } catch (final IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid reservation state");
            }
        }
    }

    record ReservationResponse(
            UUID id,
            UUID businessId,
            UUID resourceId,
            UUID customerAccountId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String state,
            OffsetDateTime holdExpiresAt) {

        static ReservationResponse from(final ReservationResult result) {
            return new ReservationResponse(
                    result.id(),
                    result.businessId(),
                    result.resourceId(),
                    result.customerAccountId(),
                    result.startAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.endAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.state().name(),
                    result.holdExpiresAt().atZone(result.businessZone()).toOffsetDateTime());
        }
    }
}

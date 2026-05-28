package io.resrv.timeslot.adapter.in.web.reservation;

import io.resrv.shared.kernel.ReservationId;
import io.resrv.timeslot.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.timeslot.application.reservation.ReservationService;
import io.resrv.timeslot.application.reservation.in.CustomerReservationDetailQuery;
import io.resrv.timeslot.application.reservation.in.CustomerReservationListQuery;
import io.resrv.timeslot.application.reservation.in.CustomerReservationPage;
import io.resrv.timeslot.application.reservation.in.CustomerReservationResult;
import io.resrv.timeslot.domain.reservation.ReservationState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/me/reservations")
class CustomerReservationWebAdapter {

    private final ReservationService service;

    CustomerReservationWebAdapter(final ReservationService service) {
        this.service = service;
    }

    @GetMapping
    CustomerReservationPageResponse list(
            final JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = CustomerReservationListQuery.DEFAULT_PAGE_VALUE) @Min(0)
                    final int page,
            @RequestParam(defaultValue = CustomerReservationListQuery.DEFAULT_SIZE_VALUE)
                    @Min(1)
                    @Max(CustomerReservationListQuery.MAX_SIZE)
                    final int size,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final Boolean upcoming) {
        final var account = AuthenticatedAccount.from(authentication);
        final var result =
                service.listCustomerReservations(
                        new CustomerReservationListQuery(
                                account.accountId(), page, size, parseState(state), upcoming));
        return CustomerReservationPageResponse.from(result);
    }

    @GetMapping("/{reservationId}")
    CustomerReservationResponse detail(
            final JwtAuthenticationToken authentication, @PathVariable final UUID reservationId) {
        final var account = AuthenticatedAccount.from(authentication);
        return CustomerReservationResponse.from(
                service.getCustomerReservation(
                        new CustomerReservationDetailQuery(
                                account.accountId(), ReservationId.of(reservationId))));
    }

    private static ReservationState parseState(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return ReservationState.valueOf(value);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid reservation state");
        }
    }

    record CustomerReservationPageResponse(
            List<CustomerReservationResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        static CustomerReservationPageResponse from(final CustomerReservationPage page) {
            return new CustomerReservationPageResponse(
                    page.items().stream().map(CustomerReservationResponse::from).toList(),
                    page.page(),
                    page.size(),
                    page.totalElements(),
                    page.totalPages());
        }
    }

    record CustomerReservationResponse(
            UUID reservationId,
            BusinessSummaryResponse business,
            ResourceSummaryResponse resource,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String state,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        static CustomerReservationResponse from(final CustomerReservationResult result) {
            return new CustomerReservationResponse(
                    result.reservationId(),
                    BusinessSummaryResponse.from(result.business()),
                    ResourceSummaryResponse.from(result.resource()),
                    result.startAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.endAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.state().name(),
                    result.holdExpiresAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.createdAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.updatedAt().atZone(result.businessZone()).toOffsetDateTime());
        }
    }

    record BusinessSummaryResponse(UUID id, String name, String slug, String timezone) {

        static BusinessSummaryResponse from(
                final CustomerReservationResult.BusinessSummary summary) {
            return new BusinessSummaryResponse(
                    summary.id(), summary.name(), summary.slug(), summary.timezone());
        }
    }

    record ResourceSummaryResponse(UUID id, String name, String slug, boolean active) {

        static ResourceSummaryResponse from(
                final CustomerReservationResult.ResourceSummary summary) {
            return new ResourceSummaryResponse(
                    summary.id(), summary.name(), summary.slug(), summary.active());
        }
    }
}

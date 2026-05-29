package io.resrv.timeslot.adapter.in.web.discovery;

import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.adapter.in.web.security.AuthenticatedAccount;
import io.resrv.timeslot.application.discovery.in.HoldReservationByBusinessSlugCommand;
import io.resrv.timeslot.application.discovery.in.HoldReservationByBusinessSlugUseCase;
import io.resrv.timeslot.application.discovery.in.PublicBusinessDiscoveryQuery;
import io.resrv.timeslot.application.discovery.in.PublicBusinessDiscoveryResult;
import io.resrv.timeslot.application.discovery.in.PublicBusinessDiscoveryUseCase;
import io.resrv.timeslot.application.discovery.in.PublicResourceDiscoveryQuery;
import io.resrv.timeslot.application.discovery.in.PublicResourceDiscoveryResult;
import io.resrv.timeslot.application.discovery.in.PublicResourceDiscoveryUseCase;
import io.resrv.timeslot.application.discovery.in.PublicSlotDiscoveryQuery;
import io.resrv.timeslot.application.discovery.in.PublicSlotDiscoveryResult;
import io.resrv.timeslot.application.discovery.in.PublicSlotDiscoveryUseCase;
import io.resrv.timeslot.application.reservation.in.ReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/public/businesses/{businessSlug}")
class PublicBookingDiscoveryWebAdapter implements PublicBookingDiscoveryApiDocs {

    private static final String SLUG_PATTERN = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$";

    private final PublicBusinessDiscoveryUseCase businessDiscoveryUseCase;
    private final PublicResourceDiscoveryUseCase resourceDiscoveryUseCase;
    private final PublicSlotDiscoveryUseCase slotDiscoveryUseCase;
    private final HoldReservationByBusinessSlugUseCase holdUseCase;

    PublicBookingDiscoveryWebAdapter(
            final PublicBusinessDiscoveryUseCase businessDiscoveryUseCase,
            final PublicResourceDiscoveryUseCase resourceDiscoveryUseCase,
            final PublicSlotDiscoveryUseCase slotDiscoveryUseCase,
            final HoldReservationByBusinessSlugUseCase holdUseCase) {
        this.businessDiscoveryUseCase = businessDiscoveryUseCase;
        this.resourceDiscoveryUseCase = resourceDiscoveryUseCase;
        this.slotDiscoveryUseCase = slotDiscoveryUseCase;
        this.holdUseCase = holdUseCase;
    }

    @Override
    @GetMapping
    public PublicBusinessResponse business(
            @PathVariable @Pattern(regexp = SLUG_PATTERN) final String businessSlug) {
        return PublicBusinessResponse.from(
                businessDiscoveryUseCase.discoverBusiness(
                        new PublicBusinessDiscoveryQuery(businessSlug)));
    }

    @Override
    @GetMapping("/resources")
    public List<PublicResourceResponse> resources(
            @PathVariable @Pattern(regexp = SLUG_PATTERN) final String businessSlug) {
        return resourceDiscoveryUseCase
                .listResources(new PublicResourceDiscoveryQuery(businessSlug))
                .stream()
                .map(PublicResourceResponse::from)
                .toList();
    }

    @Override
    @GetMapping("/resources/{resourceId}/slots")
    public List<PublicSlotResponse> slots(
            @PathVariable @Pattern(regexp = SLUG_PATTERN) final String businessSlug,
            @PathVariable final UUID resourceId,
            @RequestParam final LocalDate date) {
        return slotDiscoveryUseCase
                .listSlots(
                        new PublicSlotDiscoveryQuery(businessSlug, ResourceId.of(resourceId), date))
                .stream()
                .map(PublicSlotResponse::from)
                .toList();
    }

    @Override
    @PostMapping("/reservations")
    public PublicReservationResponse hold(
            @PathVariable @Pattern(regexp = SLUG_PATTERN) final String businessSlug,
            final JwtAuthenticationToken authentication,
            @Valid @RequestBody final HoldRequest request) {
        final var account = AuthenticatedAccount.from(authentication);
        return PublicReservationResponse.from(
                holdUseCase.holdReservation(
                        new HoldReservationByBusinessSlugCommand(
                                businessSlug,
                                ResourceId.of(request.resourceId()),
                                account.accountId(),
                                request.slotId())));
    }

    record HoldRequest(@NotNull UUID resourceId, @NotBlank String slotId) {}

    record PublicBusinessResponse(String slug, String name, String timezone) {

        static PublicBusinessResponse from(final PublicBusinessDiscoveryResult result) {
            return new PublicBusinessResponse(result.slug(), result.name(), result.timezone());
        }
    }

    record PublicResourceResponse(
            UUID resourceId, String businessSlug, String name, String description, String slug) {

        static PublicResourceResponse from(final PublicResourceDiscoveryResult result) {
            return new PublicResourceResponse(
                    result.resourceId(),
                    result.businessSlug(),
                    result.name(),
                    result.description(),
                    result.slug());
        }
    }

    record PublicSlotResponse(
            String slotId, OffsetDateTime startAt, OffsetDateTime endAt, boolean available) {

        static PublicSlotResponse from(final PublicSlotDiscoveryResult result) {
            return new PublicSlotResponse(
                    result.slotId(), result.startAt(), result.endAt(), result.available());
        }
    }

    record PublicReservationResponse(
            UUID id,
            UUID resourceId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String state,
            OffsetDateTime holdExpiresAt) {

        static PublicReservationResponse from(final ReservationResult result) {
            return new PublicReservationResponse(
                    result.id(),
                    result.resourceId(),
                    result.startAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.endAt().atZone(result.businessZone()).toOffsetDateTime(),
                    result.state().name(),
                    result.holdExpiresAt().atZone(result.businessZone()).toOffsetDateTime());
        }
    }
}

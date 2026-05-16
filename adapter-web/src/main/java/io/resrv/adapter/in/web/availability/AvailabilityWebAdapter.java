package io.resrv.adapter.in.web.availability;

import io.resrv.adapter.in.web.reservation.ReservationResponse;
import io.resrv.adapter.in.web.security.AuthenticatedPrincipal;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityUseCase;
import io.resrv.application.reservation.in.ListAvailableSlotsQuery;
import io.resrv.application.reservation.in.ListAvailableSlotsUseCase;
import io.resrv.application.reservation.in.ListResourceReservationsQuery;
import io.resrv.application.reservation.in.ListResourceReservationsUseCase;
import io.resrv.domain.resource.ResourceId;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
class AvailabilityWebAdapter implements AvailabilityApiDocs {

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

    @Override
    @PutMapping("/weekly-availability/{dayOfWeek}")
    public ResponseEntity<WeeklyAvailabilityResponse> upsertWeekly(
            @PathVariable final UUID resourceId,
            @PathVariable final int dayOfWeek,
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

    @Override
    @DeleteMapping("/weekly-availability/{dayOfWeek}")
    public ResponseEntity<Void> deleteWeekly(
            @PathVariable final UUID resourceId,
            @PathVariable final int dayOfWeek,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteWeeklyAvailabilityUseCase.delete(
                new DeleteWeeklyAvailabilityCommand(
                        principal.tenantId(), ResourceId.of(resourceId), DayOfWeek.of(dayOfWeek)));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/availability-exceptions/{date}")
    public ResponseEntity<DateAvailabilityOverrideResponse> upsertDateOverride(
            @PathVariable final UUID resourceId,
            @PathVariable final LocalDate date,
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

    @Override
    @DeleteMapping("/availability-exceptions/{date}")
    public ResponseEntity<Void> deleteDateOverride(
            @PathVariable final UUID resourceId,
            @PathVariable final LocalDate date,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteDateAvailabilityOverrideUseCase.delete(
                new DeleteDateAvailabilityOverrideCommand(
                        principal.tenantId(), ResourceId.of(resourceId), date));
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponse>> listSlots(
            @PathVariable final UUID resourceId,
            @RequestParam final LocalDate date,
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

    @Override
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> listResourceReservations(
            @PathVariable final UUID resourceId,
            @RequestParam final LocalDate date,
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
}

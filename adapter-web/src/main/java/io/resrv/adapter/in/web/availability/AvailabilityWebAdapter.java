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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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

    @PutMapping("/weekly-availability/{dayOfWeek}")
    ResponseEntity<WeeklyAvailabilityResponse> upsertWeekly(
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

    @DeleteMapping("/weekly-availability/{dayOfWeek}")
    ResponseEntity<Void> deleteWeekly(
            @PathVariable final UUID resourceId,
            @PathVariable final int dayOfWeek,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteWeeklyAvailabilityUseCase.delete(
                new DeleteWeeklyAvailabilityCommand(
                        principal.tenantId(), ResourceId.of(resourceId), DayOfWeek.of(dayOfWeek)));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/availability-exceptions/{date}")
    ResponseEntity<DateAvailabilityOverrideResponse> upsertDateOverride(
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

    @DeleteMapping("/availability-exceptions/{date}")
    ResponseEntity<Void> deleteDateOverride(
            @PathVariable final UUID resourceId,
            @PathVariable final LocalDate date,
            final JwtAuthenticationToken authentication) {
        final var principal = AuthenticatedPrincipal.from(authentication).requireAdmin();
        deleteDateAvailabilityOverrideUseCase.delete(
                new DeleteDateAvailabilityOverrideCommand(
                        principal.tenantId(), ResourceId.of(resourceId), date));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots")
    ResponseEntity<List<SlotResponse>> listSlots(
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

    @GetMapping("/reservations")
    ResponseEntity<List<ReservationResponse>> listResourceReservations(
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

    record WeeklyAvailabilityRequest(@NotNull LocalTime startTime, @NotNull LocalTime endTime) {}

    record DateAvailabilityOverrideRequest(
            boolean closed, @Nullable LocalTime startTime, @Nullable LocalTime endTime) {}

    record WeeklyAvailabilityResponse(
            UUID id,
            UUID tenantId,
            UUID resourceId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Instant createdAt,
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

    record DateAvailabilityOverrideResponse(
            UUID id,
            UUID tenantId,
            UUID resourceId,
            LocalDate date,
            boolean closed,
            @Nullable LocalTime startTime,
            @Nullable LocalTime endTime,
            Instant createdAt,
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

    record SlotResponse(Instant startAt, Instant endAt) {

        static SlotResponse from(final SlotResult result) {
            return new SlotResponse(result.startAt(), result.endAt());
        }
    }

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

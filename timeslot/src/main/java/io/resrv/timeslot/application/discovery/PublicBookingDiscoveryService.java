package io.resrv.timeslot.application.discovery;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
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
import io.resrv.timeslot.application.reservation.ReservationService;
import io.resrv.timeslot.application.reservation.in.HoldReservationCommand;
import io.resrv.timeslot.application.reservation.in.ReservationResult;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import io.resrv.timeslot.domain.slot.Slot;
import io.resrv.timeslot.domain.slot.SlotGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublicBookingDiscoveryService
        implements PublicBusinessDiscoveryUseCase,
                PublicResourceDiscoveryUseCase,
                PublicSlotDiscoveryUseCase,
                HoldReservationByBusinessSlugUseCase {

    private final PublicBookableBusinessResolver businessResolver;
    private final ResourceQueryPort resourceQueryPort;
    private final ResourceScheduleQueryPort scheduleQueryPort;
    private final ReservationQueryPort reservationQueryPort;
    private final ReservationService reservationService;
    private final Clock clock;

    public PublicBookingDiscoveryService(
            final PublicBookableBusinessResolver businessResolver,
            final ResourceQueryPort resourceQueryPort,
            final ResourceScheduleQueryPort scheduleQueryPort,
            final ReservationQueryPort reservationQueryPort,
            final ReservationService reservationService,
            final Clock clock) {
        this.businessResolver = businessResolver;
        this.resourceQueryPort = resourceQueryPort;
        this.scheduleQueryPort = scheduleQueryPort;
        this.reservationQueryPort = reservationQueryPort;
        this.reservationService = reservationService;
        this.clock = clock;
    }

    @Override
    public PublicBusinessDiscoveryResult discoverBusiness(
            final PublicBusinessDiscoveryQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var bookable = businessResolver.resolve(query.businessSlug());
        return new PublicBusinessDiscoveryResult(
                bookable.business().slug(),
                bookable.business().name(),
                bookable.business().timezone().value().getId());
    }

    @Override
    public List<PublicResourceDiscoveryResult> listResources(
            final PublicResourceDiscoveryQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var bookable = businessResolver.resolve(query.businessSlug());
        return resourceQueryPort.findActiveByBusinessId(bookable.business().id()).stream()
                .map(resource -> resourceResult(bookable.business().slug(), resource))
                .toList();
    }

    @Override
    public List<PublicSlotDiscoveryResult> listSlots(final PublicSlotDiscoveryQuery query) {
        Objects.requireNonNull(query, "Query must not be null");
        final var bookable = businessResolver.resolve(query.businessSlug());
        final var resource =
                activeResourceOrDeny(
                        bookable.business().id(), query.resourceId(), query.businessSlug());
        final var policy = resource.bookingOverrides().resolve(bookable.settings());
        final var today = LocalDate.now(clock.withZone(bookable.business().timezone().value()));
        if (query.date().isBefore(today)
                || query.date().isAfter(today.plusDays(policy.maxAdvanceBookingDays().days()))) {
            return List.of();
        }
        final var now = clock.instant();
        return generatedSlots(bookable.business().id(), resource, query.date(), bookable).stream()
                .map(
                        slot ->
                                new PublicSlotDiscoveryResult(
                                        slot.id().value(),
                                        slot.startAtBusinessTime(),
                                        slot.endAtBusinessTime(),
                                        available(slot, now)))
                .toList();
    }

    @Override
    @Transactional
    public ReservationResult holdReservation(final HoldReservationByBusinessSlugCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var bookable = businessResolver.resolve(command.businessSlug());
        activeResourceOrDeny(
                bookable.business().id(), command.resourceId(), command.businessSlug());
        return reservationService.hold(
                new HoldReservationCommand(
                        bookable.business().id(),
                        command.resourceId(),
                        command.accountId(),
                        command.slotId()));
    }

    private Resource activeResourceOrDeny(
            final BusinessId businessId, final ResourceId resourceId, final String businessSlug) {
        final var resource =
                resourceQueryPort
                        .findById(resourceId)
                        .orElseThrow(
                                () ->
                                        businessResolver.denied(
                                                businessSlug,
                                                PublicDiscoveryDenialFact.RESOURCE_NOT_FOUND));
        if (!resource.businessId().equals(businessId)) {
            throw businessResolver.denied(
                    businessSlug, PublicDiscoveryDenialFact.RESOURCE_WRONG_BUSINESS);
        }
        if (resource.status() != ResourceStatus.ACTIVE) {
            throw businessResolver.denied(
                    businessSlug, PublicDiscoveryDenialFact.RESOURCE_INACTIVE);
        }
        return resource;
    }

    private List<Slot> generatedSlots(
            final BusinessId businessId,
            final Resource resource,
            final LocalDate date,
            final PublicBookableBusinessResolver.BookableBusiness bookable) {
        final var policy = resource.bookingOverrides().resolve(bookable.settings());
        final var windows =
                scheduleQueryPort
                        .findDateOverride(businessId, resource.id(), date)
                        .map(DateResourceScheduleOverride::windows)
                        .orElseGet(
                                () ->
                                        scheduleQueryPort
                                                .findWeekly(
                                                        businessId,
                                                        resource.id(),
                                                        date.getDayOfWeek())
                                                .map(WeeklyResourceSchedule::windows)
                                                .orElse(List.of()));
        return SlotGenerator.generate(
                businessId,
                resource.id(),
                bookable.business().timezone(),
                date,
                policy.slotDuration(),
                windows);
    }

    private boolean available(final Slot slot, final Instant now) {
        return reservationQueryPort
                .findActiveBlockers(
                        slot.businessId(), slot.resourceId(), slot.startAt(), slot.endAt(), now)
                .isEmpty();
    }

    private static PublicResourceDiscoveryResult resourceResult(
            final String businessSlug, final Resource resource) {
        return new PublicResourceDiscoveryResult(
                resource.id().value(),
                businessSlug,
                resource.name().value(),
                resource.description());
    }
}

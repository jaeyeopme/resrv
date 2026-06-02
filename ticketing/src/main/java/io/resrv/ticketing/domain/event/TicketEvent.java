package io.resrv.ticketing.domain.event;

import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Objects;

public record TicketEvent(
        TicketEventId id,
        BusinessId businessId,
        TicketEventProfile profile,
        TicketSaleWindow saleWindow,
        TicketEventStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public TicketEvent {
        Objects.requireNonNull(id, "Ticket event id must not be null");
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(profile, "Ticket event profile must not be null");
        Objects.requireNonNull(saleWindow, "Ticket sale window must not be null");
        Objects.requireNonNull(status, "Ticket event status must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    public static TicketEvent create(
            final BusinessId businessId,
            final TicketEventProfile profile,
            final TicketSaleWindow saleWindow,
            final Instant now) {
        return new TicketEvent(
                TicketEventId.create(),
                businessId,
                profile,
                saleWindow,
                TicketEventStatus.ACTIVE,
                now,
                now);
    }

    public static TicketEvent reconstitute(
            final TicketEventId id,
            final BusinessId businessId,
            final TicketEventProfile profile,
            final TicketSaleWindow saleWindow,
            final TicketEventStatus status,
            final Instant createdAt,
            final Instant updatedAt) {
        return new TicketEvent(id, businessId, profile, saleWindow, status, createdAt, updatedAt);
    }

    public boolean allowsFutureClaims() {
        return status == TicketEventStatus.ACTIVE;
    }

    public TicketEvent activate(final Instant now) {
        Objects.requireNonNull(now, "Updated at must not be null");
        return new TicketEvent(
                id, businessId, profile, saleWindow, TicketEventStatus.ACTIVE, createdAt, now);
    }

    public TicketEvent deactivate(final Instant now) {
        Objects.requireNonNull(now, "Updated at must not be null");
        return new TicketEvent(
                id, businessId, profile, saleWindow, TicketEventStatus.INACTIVE, createdAt, now);
    }
}

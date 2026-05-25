package io.resrv.timeslot.domain.reservation;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import java.util.Objects;

public record Reservation(
        ReservationId id,
        BusinessId businessId,
        ResourceId resourceId,
        AccountId customerAccountId,
        Instant startAt,
        Instant endAt,
        Instant holdExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt,
        Instant releasedAt,
        Instant cancelledAt,
        ReservationCancellationActor cancelledBy,
        Instant checkedInAt,
        Instant noShowAt) {

    public Reservation {
        Objects.requireNonNull(id, "Reservation id must not be null");
        Objects.requireNonNull(businessId, "Business id must not be null");
        Objects.requireNonNull(resourceId, "Resource id must not be null");
        Objects.requireNonNull(customerAccountId, "Customer account id must not be null");
        Objects.requireNonNull(startAt, "Start at must not be null");
        Objects.requireNonNull(endAt, "End at must not be null");
        Objects.requireNonNull(holdExpiresAt, "Hold expires at must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Reservation end at must be after start at");
        }
        if ((cancelledAt == null) != (cancelledBy == null)) {
            throw new IllegalArgumentException(
                    "Cancellation actor must be present only with cancelled at");
        }
        if (terminalFactCount(releasedAt, cancelledAt, checkedInAt, noShowAt) > 1) {
            throw new IllegalArgumentException("Reservation terminal facts must not conflict");
        }
    }

    public static Reservation hold(
            final BusinessId businessId,
            final ResourceId resourceId,
            final AccountId customerAccountId,
            final Instant startAt,
            final Instant endAt,
            final Instant holdExpiresAt,
            final Instant now) {
        return new Reservation(
                ReservationId.create(),
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                now,
                now,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static Reservation reconstitute(
            final ReservationId id,
            final BusinessId businessId,
            final ResourceId resourceId,
            final AccountId customerAccountId,
            final Instant startAt,
            final Instant endAt,
            final Instant holdExpiresAt,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant confirmedAt,
            final Instant releasedAt,
            final Instant cancelledAt,
            final ReservationCancellationActor cancelledBy,
            final Instant checkedInAt,
            final Instant noShowAt) {
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                updatedAt,
                confirmedAt,
                releasedAt,
                cancelledAt,
                cancelledBy,
                checkedInAt,
                noShowAt);
    }

    public ReservationState stateAt(final Instant now) {
        Objects.requireNonNull(now, "Now must not be null");
        if (releasedAt != null) {
            return ReservationState.RELEASED;
        }
        if (checkedInAt != null) {
            return ReservationState.CHECKED_IN;
        }
        if (noShowAt != null) {
            return ReservationState.NO_SHOW;
        }
        if (cancelledAt != null && cancelledBy == ReservationCancellationActor.CUSTOMER) {
            return ReservationState.CUSTOMER_CANCELLED;
        }
        if (cancelledAt != null && cancelledBy == ReservationCancellationActor.BUSINESS) {
            return ReservationState.BUSINESS_CANCELLED;
        }
        if (confirmedAt != null) {
            return ReservationState.CONFIRMED;
        }
        if (!now.isBefore(holdExpiresAt)) {
            return ReservationState.EXPIRED;
        }
        return ReservationState.HELD;
    }

    public Reservation confirm(final Instant now) {
        requireHeld(now, "Only held reservation can be confirmed");
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                now,
                now,
                releasedAt,
                cancelledAt,
                cancelledBy,
                checkedInAt,
                noShowAt);
    }

    public Reservation release(final Instant now) {
        requireHeld(now, "Only held reservation can be released");
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                now,
                cancelledAt,
                cancelledBy,
                checkedInAt,
                noShowAt);
    }

    public Reservation cancelByCustomer(final Instant now, final Instant cutoff) {
        Objects.requireNonNull(cutoff, "Cancellation cutoff must not be null");
        requireState(
                now,
                ReservationState.CONFIRMED,
                "Only confirmed reservation can be cancelled by customer");
        if (!now.isBefore(cutoff)) {
            throw new ReservationInvalidStateException("Customer cancellation cutoff has passed");
        }
        return cancel(now, ReservationCancellationActor.CUSTOMER);
    }

    public Reservation cancelByBusiness(final Instant now) {
        final var state = stateAt(Objects.requireNonNull(now, "Now must not be null"));
        if (state != ReservationState.HELD && state != ReservationState.CONFIRMED) {
            throw new ReservationInvalidStateException(
                    "Only held or confirmed reservation can be cancelled by business");
        }
        return cancel(now, ReservationCancellationActor.BUSINESS);
    }

    public Reservation checkIn(final Instant now) {
        requireState(
                now, ReservationState.CONFIRMED, "Only confirmed reservation can be checked in");
        if (now.isBefore(startAt)) {
            throw new ReservationInvalidStateException(
                    "Reservation cannot be checked in before start at");
        }
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                releasedAt,
                cancelledAt,
                cancelledBy,
                now,
                noShowAt);
    }

    public Reservation markNoShow(final Instant now) {
        requireState(
                now,
                ReservationState.CONFIRMED,
                "Only confirmed reservation can be marked no-show");
        if (now.isBefore(endAt)) {
            throw new ReservationInvalidStateException(
                    "Reservation cannot be marked no-show before end at");
        }
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                releasedAt,
                cancelledAt,
                cancelledBy,
                checkedInAt,
                now);
    }

    private Reservation cancel(final Instant now, final ReservationCancellationActor actor) {
        return new Reservation(
                id,
                businessId,
                resourceId,
                customerAccountId,
                startAt,
                endAt,
                holdExpiresAt,
                createdAt,
                now,
                confirmedAt,
                releasedAt,
                now,
                actor,
                checkedInAt,
                noShowAt);
    }

    private void requireHeld(final Instant now, final String message) {
        final var state = stateAt(now);
        if (state == ReservationState.EXPIRED) {
            throw new ReservationHoldExpiredException("Reservation hold has expired");
        }
        if (state != ReservationState.HELD) {
            throw new ReservationInvalidStateException(message);
        }
    }

    private void requireState(
            final Instant now, final ReservationState expected, final String message) {
        if (stateAt(Objects.requireNonNull(now, "Now must not be null")) != expected) {
            throw new ReservationInvalidStateException(message);
        }
    }

    private static int terminalFactCount(final Instant... facts) {
        var count = 0;
        for (final var fact : facts) {
            if (fact != null) {
                count++;
            }
        }
        return count;
    }
}

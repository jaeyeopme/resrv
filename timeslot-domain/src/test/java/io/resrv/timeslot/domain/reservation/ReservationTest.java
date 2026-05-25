package io.resrv.timeslot.domain.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class ReservationTest {

    private static final Instant START_AT = Instant.parse("2026-05-25T01:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-05-25T01:30:00Z");
    private static final Instant HOLD_EXPIRES_AT = Instant.parse("2026-05-25T00:10:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-05-25T00:00:00Z");

    @Test
    void heldReservationExpiresByTimeWithoutStatusMutation() {
        final var reservation = hold();

        assertEquals(
                ReservationState.HELD, reservation.stateAt(Instant.parse("2026-05-25T00:09:59Z")));
        assertEquals(ReservationState.EXPIRED, reservation.stateAt(HOLD_EXPIRES_AT));
    }

    @Test
    void cannotConfirmExpiredHold() {
        final var reservation = hold();

        assertThrows(
                ReservationHoldExpiredException.class, () -> reservation.confirm(HOLD_EXPIRES_AT));
    }

    @Test
    void confirmChangesStateWithoutClearingHoldExpiryFact() {
        final var confirmed = hold().confirm(Instant.parse("2026-05-25T00:09:59Z"));

        assertEquals(
                ReservationState.CONFIRMED,
                confirmed.stateAt(Instant.parse("2026-05-25T00:11:00Z")));
        assertEquals(HOLD_EXPIRES_AT, confirmed.holdExpiresAt());
    }

    @Test
    void releaseIsOnlyAllowedForHeldReservation() {
        final var released = hold().release(Instant.parse("2026-05-25T00:09:59Z"));

        assertEquals(
                ReservationState.RELEASED, released.stateAt(Instant.parse("2026-05-25T00:11:00Z")));
        assertThrows(
                ReservationInvalidStateException.class,
                () ->
                        hold().confirm(Instant.parse("2026-05-25T00:09:59Z"))
                                .release(Instant.parse("2026-05-25T00:10:00Z")));
    }

    @Test
    void customerCancellationRequiresConfirmedReservationBeforeCutoff() {
        final var confirmed = hold().confirm(Instant.parse("2026-05-25T00:09:59Z"));
        final var cutoff = Instant.parse("2026-05-25T00:20:00Z");

        final var cancelled =
                confirmed.cancelByCustomer(Instant.parse("2026-05-25T00:19:59Z"), cutoff);

        assertEquals(
                ReservationState.CUSTOMER_CANCELLED,
                cancelled.stateAt(Instant.parse("2026-05-25T00:21:00Z")));
        assertThrows(
                ReservationInvalidStateException.class,
                () -> confirmed.cancelByCustomer(Instant.parse("2026-05-25T00:20:00Z"), cutoff));
    }

    @Test
    void businessCancellationAllowsHeldOrConfirmedReservationOnly() {
        final var heldCancelled = hold().cancelByBusiness(Instant.parse("2026-05-25T00:09:59Z"));
        final var confirmedCancelled =
                hold().confirm(Instant.parse("2026-05-25T00:09:59Z"))
                        .cancelByBusiness(Instant.parse("2026-05-25T00:10:00Z"));

        assertEquals(
                ReservationState.BUSINESS_CANCELLED,
                heldCancelled.stateAt(Instant.parse("2026-05-25T00:11:00Z")));
        assertEquals(
                ReservationState.BUSINESS_CANCELLED,
                confirmedCancelled.stateAt(Instant.parse("2026-05-25T00:11:00Z")));
        assertThrows(
                ReservationInvalidStateException.class,
                () -> hold().cancelByBusiness(Instant.parse("2026-05-25T00:10:00Z")));
    }

    @Test
    void checkInRequiresConfirmedReservationAtOrAfterStart() {
        final var confirmed = hold().confirm(Instant.parse("2026-05-25T00:09:59Z"));

        assertThrows(
                ReservationInvalidStateException.class,
                () -> confirmed.checkIn(START_AT.minusMillis(1)));

        final var checkedIn = confirmed.checkIn(START_AT);

        assertEquals(ReservationState.CHECKED_IN, checkedIn.stateAt(START_AT));
    }

    @Test
    void noShowRequiresConfirmedReservationAtOrAfterEnd() {
        final var confirmed = hold().confirm(Instant.parse("2026-05-25T00:09:59Z"));

        assertThrows(
                ReservationInvalidStateException.class,
                () -> confirmed.markNoShow(END_AT.minusMillis(1)));

        final var noShow = confirmed.markNoShow(END_AT);

        assertEquals(ReservationState.NO_SHOW, noShow.stateAt(END_AT));
    }

    @Test
    void conflictingTerminalFactsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Reservation.reconstitute(
                                ReservationId.create(),
                                BusinessId.create(),
                                ResourceId.create(),
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                HOLD_EXPIRES_AT,
                                CREATED_AT,
                                CREATED_AT,
                                Instant.parse("2026-05-25T00:09:59Z"),
                                null,
                                Instant.parse("2026-05-25T00:20:00Z"),
                                ReservationCancellationActor.CUSTOMER,
                                START_AT,
                                null));
    }

    private static Reservation hold() {
        return Reservation.hold(
                BusinessId.create(),
                ResourceId.create(),
                AccountId.create(),
                START_AT,
                END_AT,
                HOLD_EXPIRES_AT,
                CREATED_AT);
    }
}

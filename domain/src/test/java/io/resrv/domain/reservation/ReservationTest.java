package io.resrv.domain.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final ResourceId RESOURCE_ID = ResourceId.create();
    private static final CustomerId CUSTOMER_ID = CustomerId.create();
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2025-01-03T10:00:00Z");
    private static final Instant END_AT = Instant.parse("2025-01-03T11:00:00Z");
    private static final Instant HOLD_EXPIRES_AT = Instant.parse("2025-01-01T00:15:00Z");
    private static final Instant LATER = Instant.parse("2025-01-01T00:05:00Z");

    @Test
    void holdCreatesHeldReservation() {
        final var reservation = heldReservation();

        assertNotNull(reservation.id());
        assertEquals(TENANT_ID, reservation.tenantId());
        assertEquals(RESOURCE_ID, reservation.resourceId());
        assertEquals(CUSTOMER_ID, reservation.customerId());
        assertEquals(START_AT, reservation.startAt());
        assertEquals(END_AT, reservation.endAt());
        assertEquals(ReservationStatus.HELD, reservation.status());
        assertEquals(HOLD_EXPIRES_AT, reservation.holdExpiresAt());
        assertEquals(NOW, reservation.createdAt());
        assertEquals(NOW, reservation.updatedAt());
        assertNull(reservation.confirmedAt());
        assertNull(reservation.cancelledAt());
    }

    @Test
    void holdRejectsExpiredHoldAndInvalidReservationRange() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Reservation.hold(
                                TENANT_ID, RESOURCE_ID, CUSTOMER_ID, START_AT, END_AT, NOW, NOW));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Reservation.hold(
                                TENANT_ID,
                                RESOURCE_ID,
                                CUSTOMER_ID,
                                START_AT,
                                START_AT,
                                HOLD_EXPIRES_AT,
                                NOW));
    }

    @Test
    void confirmHeldReservationAndRemainIdempotent() {
        final var confirmed = heldReservation().confirm(LATER);

        assertEquals(ReservationStatus.CONFIRMED, confirmed.status());
        assertEquals(LATER, confirmed.updatedAt());
        assertEquals(LATER, confirmed.confirmedAt());
        assertSame(confirmed, confirmed.confirm(LATER.plusSeconds(1)));
    }

    @Test
    void confirmRejectsExpiredHoldAndInvalidState() {
        assertThrows(
                ReservationHoldExpiredException.class,
                () -> heldReservation().confirm(HOLD_EXPIRES_AT));

        final var expired = heldReservation().expire(HOLD_EXPIRES_AT);
        assertThrows(ReservationInvalidStateException.class, () -> expired.confirm(LATER));
    }

    @Test
    void expireOnlyTransitionsExpiredHolds() {
        final var held = heldReservation();

        assertSame(held, held.expire(LATER));
        final var expired = held.expire(HOLD_EXPIRES_AT);
        final var confirmed = held.confirm(LATER);

        assertEquals(ReservationStatus.EXPIRED, expired.status());
        assertEquals(HOLD_EXPIRES_AT, expired.updatedAt());
        assertSame(confirmed, confirmed.expire(HOLD_EXPIRES_AT));
    }

    @Test
    void cancelByCustomerRequiresOpenCancellationWindow() {
        final var confirmed = heldReservation().confirm(LATER);
        final var cancelled = confirmed.cancelByCustomer(LATER, START_AT);

        assertEquals(ReservationStatus.CUSTOMER_CANCELLED, cancelled.status());
        assertEquals(LATER, cancelled.updatedAt());
        assertEquals(LATER, cancelled.cancelledAt());
        assertEquals(confirmed.confirmedAt(), cancelled.confirmedAt());

        assertThrows(
                ReservationCancellationClosedException.class,
                () -> confirmed.cancelByCustomer(START_AT, START_AT));
        assertThrows(
                ReservationInvalidStateException.class,
                () -> cancelled.cancelByCustomer(LATER, START_AT));
    }

    @Test
    void cancelByAdminAllowsHeldOrConfirmedOnly() {
        final var held = heldReservation();
        final var cancelledHeld = held.cancelByAdmin(LATER);
        final var cancelledConfirmed = held.confirm(LATER).cancelByAdmin(LATER.plusSeconds(1));

        assertEquals(ReservationStatus.ADMIN_CANCELLED, cancelledHeld.status());
        assertEquals(LATER, cancelledHeld.cancelledAt());
        assertEquals(ReservationStatus.ADMIN_CANCELLED, cancelledConfirmed.status());
        assertEquals(LATER.plusSeconds(1), cancelledConfirmed.cancelledAt());
        assertThrows(
                ReservationInvalidStateException.class, () -> cancelledHeld.cancelByAdmin(LATER));
    }

    @Test
    void checkInConfirmedReservationAtOrAfterStart() {
        final var confirmed = heldReservation().confirm(LATER);
        final var checkedIn = confirmed.checkIn(START_AT);

        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.status());
        assertEquals(START_AT, checkedIn.updatedAt());
        assertEquals(confirmed.confirmedAt(), checkedIn.confirmedAt());
        assertNull(checkedIn.cancelledAt());

        assertThrows(
                ReservationInvalidStateException.class,
                () -> confirmed.checkIn(START_AT.minusSeconds(1)));
        assertThrows(
                ReservationInvalidStateException.class, () -> heldReservation().checkIn(START_AT));
        assertThrows(
                ReservationInvalidStateException.class,
                () -> checkedIn.checkIn(START_AT.plusSeconds(1)));
    }

    @Test
    void markNoShowConfirmedReservationAtOrAfterEnd() {
        final var confirmed = heldReservation().confirm(LATER);
        final var noShow = confirmed.markNoShow(END_AT);

        assertEquals(ReservationStatus.NO_SHOW, noShow.status());
        assertEquals(END_AT, noShow.updatedAt());
        assertEquals(confirmed.confirmedAt(), noShow.confirmedAt());
        assertNull(noShow.cancelledAt());

        assertThrows(
                ReservationInvalidStateException.class,
                () -> confirmed.markNoShow(END_AT.minusSeconds(1)));
        assertThrows(
                ReservationInvalidStateException.class, () -> heldReservation().markNoShow(END_AT));
        assertThrows(
                ReservationInvalidStateException.class,
                () -> noShow.markNoShow(END_AT.plusSeconds(1)));
    }

    private static Reservation heldReservation() {
        return Reservation.hold(
                TENANT_ID, RESOURCE_ID, CUSTOMER_ID, START_AT, END_AT, HOLD_EXPIRES_AT, NOW);
    }
}

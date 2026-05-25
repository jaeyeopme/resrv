package io.resrv.shared.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SharedIdTest {

    @Test
    void accountId_wrapsUuid() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid, AccountId.of(uuid).value());
    }

    @Test
    void idsWithSameUuidAreEqualInsideSameType() {
        final var uuid = UUID.randomUUID();
        assertEquals(BusinessId.of(uuid), BusinessId.of(uuid));
    }

    @Test
    void resourceId_wrapsUuid() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid, ResourceId.of(uuid).value());
    }

    @Test
    void reservationId_wrapsUuid() {
        final var uuid = UUID.randomUUID();
        assertEquals(uuid, ReservationId.of(uuid).value());
    }

    @Test
    void createGeneratesUuid() {
        assertNotNull(AccountId.create().value());
        assertNotNull(BusinessId.create().value());
        assertNotNull(ResourceId.create().value());
        assertNotNull(ReservationId.create().value());
    }

    @Test
    void differentIdTypesAreNotEqualEvenWithSameUuid() {
        final var uuid = UUID.randomUUID();
        assertNotEquals(AccountId.of(uuid), BusinessId.of(uuid));
    }

    @Test
    void idCannotWrapNull() {
        assertThrows(NullPointerException.class, () -> ResourceId.of(null));
    }

    @Test
    void nullIdsHaveSpecificMessages() {
        assertEquals(
                "Account id must not be null",
                assertThrows(NullPointerException.class, () -> AccountId.of(null)).getMessage());
        assertEquals(
                "Business id must not be null",
                assertThrows(NullPointerException.class, () -> BusinessId.of(null)).getMessage());
        assertEquals(
                "Resource id must not be null",
                assertThrows(NullPointerException.class, () -> ResourceId.of(null)).getMessage());
        assertEquals(
                "Reservation id must not be null",
                assertThrows(NullPointerException.class, () -> ReservationId.of(null))
                        .getMessage());
    }
}

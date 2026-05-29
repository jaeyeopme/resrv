package io.resrv.platform.domain.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessMembershipTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Instant CREATED_AT = Instant.parse("2026-05-29T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-05-29T01:00:00Z");

    @Test
    void ownerAndStaffStartActive() {
        final var owner = BusinessMembership.owner(ACCOUNT_ID, BUSINESS_ID, CREATED_AT);
        final var staff = BusinessMembership.staff(ACCOUNT_ID, BUSINESS_ID, CREATED_AT);

        assertNotNull(owner.id());
        assertEquals(BusinessRole.OWNER, owner.role());
        assertTrue(owner.active());
        assertEquals(CREATED_AT, owner.createdAt());
        assertEquals(CREATED_AT, owner.updatedAt());
        assertNull(owner.disabledAt());

        assertEquals(BusinessRole.STAFF, staff.role());
        assertTrue(staff.active());
        assertNull(staff.disabledAt());
    }

    @Test
    void reconstitutePreservesCurrentState() {
        final var membership =
                BusinessMembership.reconstitute(
                        UUID.randomUUID(),
                        ACCOUNT_ID,
                        BUSINESS_ID,
                        BusinessRole.STAFF,
                        false,
                        CREATED_AT,
                        LATER,
                        LATER);

        assertEquals(BusinessRole.STAFF, membership.role());
        assertFalse(membership.active());
        assertEquals(CREATED_AT, membership.createdAt());
        assertEquals(LATER, membership.updatedAt());
        assertEquals(LATER, membership.disabledAt());
    }

    @Test
    void reactivationReturnsActiveStaffWithSameIdentity() {
        final var inactive =
                BusinessMembership.reconstitute(
                        UUID.randomUUID(),
                        ACCOUNT_ID,
                        BUSINESS_ID,
                        BusinessRole.OWNER,
                        false,
                        CREATED_AT,
                        CREATED_AT,
                        CREATED_AT);

        final var reactivated = inactive.reactivateAsStaff(LATER);

        assertEquals(inactive.id(), reactivated.id());
        assertEquals(BusinessRole.STAFF, reactivated.role());
        assertTrue(reactivated.active());
        assertEquals(LATER, reactivated.updatedAt());
        assertNull(reactivated.disabledAt());
    }

    @Test
    void activeMembershipCannotBeReactivated() {
        final var membership = BusinessMembership.staff(ACCOUNT_ID, BUSINESS_ID, CREATED_AT);

        assertThrows(IllegalStateException.class, () -> membership.reactivateAsStaff(LATER));
    }

    @Test
    void activeMembershipCanChangeRoleAndDisable() {
        final var staff = BusinessMembership.staff(ACCOUNT_ID, BUSINESS_ID, CREATED_AT);

        final var owner = staff.changeRole(BusinessRole.OWNER, LATER);
        final var disabled = owner.disable(LATER);

        assertEquals(staff.id(), owner.id());
        assertEquals(BusinessRole.OWNER, owner.role());
        assertTrue(owner.active());
        assertEquals(LATER, owner.updatedAt());

        assertFalse(disabled.active());
        assertEquals(BusinessRole.OWNER, disabled.role());
        assertEquals(LATER, disabled.updatedAt());
        assertEquals(LATER, disabled.disabledAt());
    }

    @Test
    void inactiveMembershipCannotChangeRoleAndDisableIsNoOp() {
        final var inactive =
                BusinessMembership.staff(ACCOUNT_ID, BUSINESS_ID, CREATED_AT).disable(LATER);

        assertThrows(
                IllegalStateException.class, () -> inactive.changeRole(BusinessRole.OWNER, LATER));
        assertEquals(inactive, inactive.disable(LATER));
    }

    @Test
    void invalidTimestampsAndDisabledStateAreRejected() {
        final var id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BusinessMembership.reconstitute(
                                id,
                                ACCOUNT_ID,
                                BUSINESS_ID,
                                BusinessRole.STAFF,
                                true,
                                LATER,
                                CREATED_AT,
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BusinessMembership.reconstitute(
                                id,
                                ACCOUNT_ID,
                                BUSINESS_ID,
                                BusinessRole.STAFF,
                                true,
                                CREATED_AT,
                                CREATED_AT,
                                LATER));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BusinessMembership.reconstitute(
                                id,
                                ACCOUNT_ID,
                                BUSINESS_ID,
                                BusinessRole.STAFF,
                                false,
                                CREATED_AT,
                                CREATED_AT,
                                null));
    }
}

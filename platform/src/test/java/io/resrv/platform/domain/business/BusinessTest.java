package io.resrv.platform.domain.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createStartsActive() {
        final var business =
                Business.create(
                        new BusinessName("Owner Studio"),
                        new BusinessSlug("owner-studio"),
                        Timezone.of("Asia/Seoul"),
                        NOW);

        assertNotNull(business.id());
        assertEquals(BusinessStatus.ACTIVE, business.status());
        assertTrue(business.active());
        assertEquals(NOW, business.createdAt());
    }

    @Test
    void nameValueTrimmed() {
        final var name = new BusinessName("  Owner Studio  ");

        assertEquals("Owner Studio", name.value());
    }

    @Test
    void invalidNameRejected() {
        final var nullException =
                assertThrows(IllegalArgumentException.class, () -> new BusinessName(null));
        final var blankException =
                assertThrows(IllegalArgumentException.class, () -> new BusinessName(" "));
        final var tooLongException =
                assertThrows(
                        IllegalArgumentException.class, () -> new BusinessName("x".repeat(101)));

        assertEquals("Business name must be 1-100 characters", nullException.getMessage());
        assertEquals("Business name must be 1-100 characters", blankException.getMessage());
        assertEquals("Business name must be 1-100 characters", tooLongException.getMessage());
    }

    @Test
    void invalidSlugRejected() {
        final var nullException =
                assertThrows(IllegalArgumentException.class, () -> new BusinessSlug(null));
        final var uppercaseException =
                assertThrows(IllegalArgumentException.class, () -> new BusinessSlug("Owner"));
        final var tooShortException =
                assertThrows(IllegalArgumentException.class, () -> new BusinessSlug("ab"));

        assertEquals(
                "Business slug must be 3-63 lowercase URL characters", nullException.getMessage());
        assertEquals(
                "Business slug must be 3-63 lowercase URL characters",
                uppercaseException.getMessage());
        assertEquals(
                "Business slug must be 3-63 lowercase URL characters",
                tooShortException.getMessage());
    }

    @Test
    void reconstitutePreservesValuesAndInactiveIsNotActive() {
        final var id = BusinessId.create();

        final var business =
                Business.reconstitute(
                        id,
                        new BusinessName("Owner Studio"),
                        new BusinessSlug("owner-studio"),
                        Timezone.of("Asia/Seoul"),
                        BusinessStatus.INACTIVE,
                        NOW);

        assertEquals(id, business.id());
        assertEquals("Owner Studio", business.name().value());
        assertEquals("owner-studio", business.slug().value());
        assertEquals("Asia/Seoul", business.timezone().value().getId());
        assertEquals(BusinessStatus.INACTIVE, business.status());
        assertEquals(NOW, business.createdAt());
        assertFalse(business.active());
    }

    @Test
    void duplicateSlugExceptionContainsSlug() {
        final var exception =
                new BusinessSlugAlreadyExistsException(new BusinessSlug("owner-studio"));

        assertEquals("Business slug already exists: owner-studio", exception.getMessage());
    }

    @Test
    void ownerMembershipStartsActive() {
        final var accountId = AccountId.create();
        final var businessId = BusinessId.create();

        final var membership = BusinessMembership.owner(accountId, businessId, NOW);

        assertNotNull(membership.id());
        assertEquals(accountId, membership.accountId());
        assertEquals(businessId, membership.businessId());
        assertEquals(BusinessRole.OWNER, membership.role());
        assertTrue(membership.active());
        assertEquals(NOW, membership.createdAt());
    }

    @Test
    void membershipReconstitutePreservesInactiveStaff() {
        final var id = UUID.randomUUID();
        final var accountId = AccountId.create();
        final var businessId = BusinessId.create();

        final var membership =
                BusinessMembership.reconstitute(
                        id, accountId, businessId, BusinessRole.STAFF, false, NOW);

        assertEquals(id, membership.id());
        assertEquals(accountId, membership.accountId());
        assertEquals(businessId, membership.businessId());
        assertEquals(BusinessRole.STAFF, membership.role());
        assertFalse(membership.active());
        assertEquals(NOW, membership.createdAt());
    }
}

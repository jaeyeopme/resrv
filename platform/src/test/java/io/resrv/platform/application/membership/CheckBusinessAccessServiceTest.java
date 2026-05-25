package io.resrv.platform.application.membership;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class CheckBusinessAccessServiceTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    private BusinessMembershipQueryPort membershipQueryPort;
    private CheckBusinessAccessService service;

    @BeforeEach
    void setUp() {
        membershipQueryPort = mock(BusinessMembershipQueryPort.class);
        service = new CheckBusinessAccessService(membershipQueryPort);
    }

    @Test
    void activeOwnerOrStaffHasBusinessAccess() {
        when(membershipQueryPort.findActiveByAccountIdAndBusinessId(ACCOUNT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(membership(BusinessRole.OWNER)));

        assertTrue(service.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));

        when(membershipQueryPort.findActiveByAccountIdAndBusinessId(ACCOUNT_ID, BUSINESS_ID))
                .thenReturn(Optional.of(membership(BusinessRole.STAFF)));

        assertTrue(service.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }

    @Test
    void missingMembershipDoesNotHaveBusinessAccess() {
        when(membershipQueryPort.findActiveByAccountIdAndBusinessId(ACCOUNT_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());

        assertFalse(service.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }

    private static BusinessMembership membership(final BusinessRole role) {
        return BusinessMembership.reconstitute(
                UUID.randomUUID(), ACCOUNT_ID, BUSINESS_ID, role, true, NOW);
    }
}

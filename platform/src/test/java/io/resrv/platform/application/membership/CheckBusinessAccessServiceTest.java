package io.resrv.platform.application.membership;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.account.AccountStatus;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessStatus;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class CheckBusinessAccessServiceTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    private AccountQueryPort accountQueryPort;
    private BusinessQueryPort businessQueryPort;
    private BusinessMembershipQueryPort membershipQueryPort;
    private CheckBusinessAccessService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = mock(AccountQueryPort.class);
        businessQueryPort = mock(BusinessQueryPort.class);
        membershipQueryPort = mock(BusinessMembershipQueryPort.class);
        service =
                new CheckBusinessAccessService(
                        accountQueryPort, businessQueryPort, membershipQueryPort);
        when(accountQueryPort.findById(ACCOUNT_ID)).thenReturn(Optional.of(activeAccount()));
        when(businessQueryPort.findById(BUSINESS_ID)).thenReturn(Optional.of(activeBusiness()));
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

    @Test
    void inactiveAccountDoesNotHaveBusinessAccess() {
        when(accountQueryPort.findById(ACCOUNT_ID)).thenReturn(Optional.of(disabledAccount()));

        assertFalse(service.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }

    @Test
    void inactiveBusinessDoesNotHaveBusinessAccess() {
        when(businessQueryPort.findById(BUSINESS_ID)).thenReturn(Optional.of(inactiveBusiness()));

        assertFalse(service.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }

    private static BusinessMembership membership(final BusinessRole role) {
        return BusinessMembership.reconstitute(
                UUID.randomUUID(), ACCOUNT_ID, BUSINESS_ID, role, true, NOW);
    }

    private static Account activeAccount() {
        return account(AccountStatus.ACTIVE);
    }

    private static Account disabledAccount() {
        return account(AccountStatus.DISABLED);
    }

    private static Account account(final AccountStatus status) {
        return Account.reconstitute(
                ACCOUNT_ID,
                new AccountEmail("owner@example.com"),
                new AccountName("Owner"),
                "$argon2id$test",
                status,
                NOW);
    }

    private static Business activeBusiness() {
        return business(BusinessStatus.ACTIVE);
    }

    private static Business inactiveBusiness() {
        return business(BusinessStatus.INACTIVE);
    }

    private static Business business(final BusinessStatus status) {
        return Business.reconstitute(
                BUSINESS_ID,
                new BusinessName("Salon A"),
                new BusinessSlug("salon-a"),
                Timezone.of("Asia/Seoul"),
                status,
                NOW);
    }
}

package io.resrv.platform.application.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.in.DisableMembershipCommand;
import io.resrv.platform.application.membership.in.GrantStaffMembershipCommand;
import io.resrv.platform.application.membership.in.ListBusinessMembershipsQuery;
import io.resrv.platform.application.membership.in.MembershipAuditHistoryQuery;
import io.resrv.platform.application.membership.in.UpdateMembershipRoleCommand;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
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
import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.platform.domain.membership.MembershipAuditAction;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

final class BusinessMembershipAdministrationServiceTest {

    private static final AccountId OWNER_ID = AccountId.create();
    private static final AccountId STAFF_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-05-28T00:00:00Z");

    private AccountQueryPort accountQueryPort;
    private BusinessQueryPort businessQueryPort;
    private BusinessMembershipCommandPort commandPort;
    private BusinessMembershipQueryPort queryPort;
    private BusinessMembershipAdministrationService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = mock(AccountQueryPort.class);
        businessQueryPort = mock(BusinessQueryPort.class);
        commandPort = mock(BusinessMembershipCommandPort.class);
        queryPort = mock(BusinessMembershipQueryPort.class);
        service =
                new BusinessMembershipAdministrationService(
                        accountQueryPort,
                        businessQueryPort,
                        commandPort,
                        queryPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        when(accountQueryPort.findById(OWNER_ID))
                .thenReturn(
                        Optional.of(account(OWNER_ID, "owner@example.com", AccountStatus.ACTIVE)));
        when(accountQueryPort.findById(STAFF_ID))
                .thenReturn(
                        Optional.of(account(STAFF_ID, "staff@example.com", AccountStatus.ACTIVE)));
        when(accountQueryPort.findByEmail(new AccountEmail("staff@example.com")))
                .thenReturn(
                        Optional.of(account(STAFF_ID, "staff@example.com", AccountStatus.ACTIVE)));
        when(businessQueryPort.findById(BUSINESS_ID)).thenReturn(Optional.of(activeBusiness()));
        when(queryPort.findActiveByAccountIdAndBusinessId(OWNER_ID, BUSINESS_ID))
                .thenReturn(Optional.of(BusinessMembership.owner(OWNER_ID, BUSINESS_ID, EARLIER)));
    }

    @Test
    void grantStaffCreatesMembershipAndAuditEntry() {
        when(queryPort.findByAccountIdAndBusinessId(STAFF_ID, BUSINESS_ID))
                .thenReturn(Optional.empty());

        final var response =
                service.grantStaff(
                        new GrantStaffMembershipCommand(
                                OWNER_ID, BUSINESS_ID, "staff@example.com"));

        assertEquals(STAFF_ID.value(), response.account().accountId());
        assertEquals(BusinessRole.STAFF, response.role());
        assertTrue(response.active());
        final var membershipCaptor = ArgumentCaptor.forClass(BusinessMembership.class);
        verify(commandPort).save(membershipCaptor.capture());
        assertEquals(BusinessRole.STAFF, membershipCaptor.getValue().role());
        final var auditCaptor = ArgumentCaptor.forClass(BusinessMembershipAuditEntry.class);
        verify(commandPort).saveAuditEntry(auditCaptor.capture());
        assertEquals(MembershipAuditAction.GRANTED, auditCaptor.getValue().action());
    }

    @Test
    void grantRejectsNonOwnerMissingOrInactiveTargetAndDuplicateActiveMembership() {
        when(queryPort.findActiveByAccountIdAndBusinessId(OWNER_ID, BUSINESS_ID))
                .thenReturn(Optional.of(BusinessMembership.staff(OWNER_ID, BUSINESS_ID, EARLIER)));
        assertThrows(
                MembershipAdministrationDeniedException.class,
                () ->
                        service.grantStaff(
                                new GrantStaffMembershipCommand(
                                        OWNER_ID, BUSINESS_ID, "staff@example.com")));

        when(queryPort.findActiveByAccountIdAndBusinessId(OWNER_ID, BUSINESS_ID))
                .thenReturn(Optional.of(BusinessMembership.owner(OWNER_ID, BUSINESS_ID, EARLIER)));
        when(accountQueryPort.findByEmail(new AccountEmail("missing@example.com")))
                .thenReturn(Optional.empty());
        assertThrows(
                TargetAccountUnavailableException.class,
                () ->
                        service.grantStaff(
                                new GrantStaffMembershipCommand(
                                        OWNER_ID, BUSINESS_ID, "missing@example.com")));

        when(accountQueryPort.findByEmail(new AccountEmail("disabled@example.com")))
                .thenReturn(
                        Optional.of(
                                account(STAFF_ID, "disabled@example.com", AccountStatus.DISABLED)));
        assertThrows(
                TargetAccountUnavailableException.class,
                () ->
                        service.grantStaff(
                                new GrantStaffMembershipCommand(
                                        OWNER_ID, BUSINESS_ID, "disabled@example.com")));

        when(queryPort.findByAccountIdAndBusinessId(STAFF_ID, BUSINESS_ID))
                .thenReturn(Optional.of(BusinessMembership.staff(STAFF_ID, BUSINESS_ID, EARLIER)));
        assertThrows(
                DuplicateActiveMembershipException.class,
                () ->
                        service.grantStaff(
                                new GrantStaffMembershipCommand(
                                        OWNER_ID, BUSINESS_ID, "staff@example.com")));
    }

    @Test
    void grantReactivatesDisabledMembership() {
        final var disabled =
                BusinessMembership.staff(STAFF_ID, BUSINESS_ID, EARLIER).disable(EARLIER);
        when(queryPort.findByAccountIdAndBusinessId(STAFF_ID, BUSINESS_ID))
                .thenReturn(Optional.of(disabled));

        final var response =
                service.grantStaff(
                        new GrantStaffMembershipCommand(
                                OWNER_ID, BUSINESS_ID, "staff@example.com"));

        assertEquals(disabled.id(), response.membershipId());
        assertTrue(response.active());
        final var auditCaptor = ArgumentCaptor.forClass(BusinessMembershipAuditEntry.class);
        verify(commandPort).saveAuditEntry(auditCaptor.capture());
        assertEquals(MembershipAuditAction.REACTIVATED, auditCaptor.getValue().action());
        assertFalse(auditCaptor.getValue().previousActive());
    }

    @Test
    void ownerCanListMembershipsAndAuditHistory() {
        final var ownerMembership = BusinessMembership.owner(OWNER_ID, BUSINESS_ID, EARLIER);
        final var staffMembership = BusinessMembership.staff(STAFF_ID, BUSINESS_ID, EARLIER);
        when(queryPort.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(ownerMembership, staffMembership.disable(NOW)));
        when(queryPort.findAuditEntriesByBusinessId(BUSINESS_ID))
                .thenReturn(
                        List.of(
                                BusinessMembershipAuditEntry.create(
                                        staffMembership.id(),
                                        BUSINESS_ID,
                                        OWNER_ID,
                                        STAFF_ID,
                                        MembershipAuditAction.DISABLED,
                                        BusinessRole.STAFF,
                                        true,
                                        BusinessRole.STAFF,
                                        false,
                                        NOW)));

        final var memberships =
                service.listMemberships(new ListBusinessMembershipsQuery(OWNER_ID, BUSINESS_ID));
        final var audit =
                service.listAuditHistory(new MembershipAuditHistoryQuery(OWNER_ID, BUSINESS_ID));

        assertEquals(2, memberships.size());
        assertEquals("staff@example.com", memberships.get(1).account().email());
        assertFalse(memberships.get(1).active());
        assertEquals(1, audit.size());
        assertEquals(MembershipAuditAction.DISABLED, audit.getFirst().action());
        assertEquals("owner@example.com", audit.getFirst().actor().email());
        assertEquals("staff@example.com", audit.getFirst().target().email());
    }

    @Test
    void updateRoleAndDisableApplyAuditedStateChanges() {
        final var staffMembership = BusinessMembership.staff(STAFF_ID, BUSINESS_ID, EARLIER);
        when(queryPort.findById(staffMembership.id())).thenReturn(Optional.of(staffMembership));
        when(queryPort.countActiveByBusinessIdAndRole(BUSINESS_ID, BusinessRole.OWNER))
                .thenReturn(2L);

        final var promoted =
                service.updateRole(
                        new UpdateMembershipRoleCommand(
                                OWNER_ID, BUSINESS_ID, staffMembership.id(), BusinessRole.OWNER));
        assertEquals(BusinessRole.OWNER, promoted.role());

        when(queryPort.findById(staffMembership.id())).thenReturn(Optional.of(staffMembership));
        final var disabled =
                service.disable(
                        new DisableMembershipCommand(OWNER_ID, BUSINESS_ID, staffMembership.id()));
        assertFalse(disabled.active());

        verify(commandPort, Mockito.times(2)).saveAuditEntry(any());
    }

    @Test
    void updateAndDisableRejectWrongBusinessAndLastOwner() {
        final var otherBusinessId = BusinessId.create();
        final var wrongBusinessMembership =
                BusinessMembership.owner(STAFF_ID, otherBusinessId, EARLIER);
        final var membership = BusinessMembership.owner(STAFF_ID, BUSINESS_ID, EARLIER);
        when(queryPort.findById(wrongBusinessMembership.id()))
                .thenReturn(Optional.of(wrongBusinessMembership));
        when(queryPort.findById(membership.id())).thenReturn(Optional.of(membership));
        when(queryPort.countActiveByBusinessIdAndRole(BUSINESS_ID, BusinessRole.OWNER))
                .thenReturn(1L);

        assertThrows(
                BusinessMembershipNotFoundException.class,
                () ->
                        service.updateRole(
                                new UpdateMembershipRoleCommand(
                                        OWNER_ID,
                                        BUSINESS_ID,
                                        wrongBusinessMembership.id(),
                                        BusinessRole.STAFF)));
        assertThrows(
                LastOwnerMembershipException.class,
                () ->
                        service.updateRole(
                                new UpdateMembershipRoleCommand(
                                        OWNER_ID,
                                        BUSINESS_ID,
                                        membership.id(),
                                        BusinessRole.STAFF)));
        assertThrows(
                LastOwnerMembershipException.class,
                () ->
                        service.disable(
                                new DisableMembershipCommand(
                                        OWNER_ID, BUSINESS_ID, membership.id())));
    }

    @Test
    void alreadyInactiveDisableReturnsStateWithoutAudit() {
        final var inactive =
                BusinessMembership.staff(STAFF_ID, BUSINESS_ID, EARLIER).disable(EARLIER);
        when(queryPort.findById(inactive.id())).thenReturn(Optional.of(inactive));

        final var response =
                service.disable(new DisableMembershipCommand(OWNER_ID, BUSINESS_ID, inactive.id()));

        assertFalse(response.active());
        verify(commandPort, never()).saveAuditEntry(any());
    }

    private static Account account(
            final AccountId accountId, final String email, final AccountStatus status) {
        return Account.reconstitute(
                accountId,
                new AccountEmail(email),
                new AccountName(email.substring(0, email.indexOf('@'))),
                "$argon2id$test",
                status,
                EARLIER);
    }

    private static Business activeBusiness() {
        return Business.reconstitute(
                BUSINESS_ID,
                new BusinessName("Salon A"),
                new BusinessSlug("salon-a"),
                Timezone.of("Asia/Seoul"),
                BusinessStatus.ACTIVE,
                EARLIER);
    }
}

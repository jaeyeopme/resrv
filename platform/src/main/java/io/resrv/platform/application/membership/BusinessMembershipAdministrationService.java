package io.resrv.platform.application.membership;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.in.BusinessMembershipAdministrationUseCase;
import io.resrv.platform.application.membership.in.BusinessMembershipListItem;
import io.resrv.platform.application.membership.in.DisableMembershipCommand;
import io.resrv.platform.application.membership.in.GrantStaffMembershipCommand;
import io.resrv.platform.application.membership.in.ListBusinessMembershipsQuery;
import io.resrv.platform.application.membership.in.MembershipAccountSummary;
import io.resrv.platform.application.membership.in.MembershipAdministrationResponse;
import io.resrv.platform.application.membership.in.MembershipAuditHistoryItem;
import io.resrv.platform.application.membership.in.MembershipAuditHistoryQuery;
import io.resrv.platform.application.membership.in.UpdateMembershipRoleCommand;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.application.membership.out.BusinessMembershipQueryPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.platform.domain.membership.MembershipAuditAction;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessMembershipAdministrationService
        implements BusinessMembershipAdministrationUseCase {

    private final AccountQueryPort accountQueryPort;
    private final BusinessQueryPort businessQueryPort;
    private final BusinessMembershipCommandPort membershipCommandPort;
    private final BusinessMembershipQueryPort membershipQueryPort;
    private final Clock clock;

    public BusinessMembershipAdministrationService(
            final AccountQueryPort accountQueryPort,
            final BusinessQueryPort businessQueryPort,
            final BusinessMembershipCommandPort membershipCommandPort,
            final BusinessMembershipQueryPort membershipQueryPort,
            final Clock clock) {
        this.accountQueryPort = accountQueryPort;
        this.businessQueryPort = businessQueryPort;
        this.membershipCommandPort = membershipCommandPort;
        this.membershipQueryPort = membershipQueryPort;
        this.clock = clock;
    }

    @Override
    public MembershipAdministrationResponse grantStaff(final GrantStaffMembershipCommand command) {
        requireActiveOwner(command.actorAccountId(), command.businessId());
        final var target =
                accountQueryPort
                        .findByEmail(new AccountEmail(command.targetAccountEmail()))
                        .filter(Account::active)
                        .orElseThrow(TargetAccountUnavailableException::new);
        final var now = clock.instant();
        final var existing =
                membershipQueryPort.findByAccountIdAndBusinessId(target.id(), command.businessId());
        final BusinessMembership membership;
        final MembershipAuditAction action;
        final BusinessRole previousRole;
        final Boolean previousActive;
        if (existing.isPresent()) {
            final var previous = existing.orElseThrow();
            if (previous.active()) {
                throw new DuplicateActiveMembershipException();
            }
            membership = previous.reactivateAsStaff(now);
            action = MembershipAuditAction.REACTIVATED;
            previousRole = previous.role();
            previousActive = previous.active();
        } else {
            membership = BusinessMembership.staff(target.id(), command.businessId(), now);
            action = MembershipAuditAction.GRANTED;
            previousRole = null;
            previousActive = null;
        }
        membershipCommandPort.save(membership);
        membershipCommandPort.saveAuditEntry(
                auditEntry(
                        membership,
                        command.actorAccountId(),
                        action,
                        previousRole,
                        previousActive,
                        membership.role(),
                        membership.active(),
                        now));
        return response(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessMembershipListItem> listMemberships(
            final ListBusinessMembershipsQuery query) {
        requireActiveOwner(query.actorAccountId(), query.businessId());
        return membershipQueryPort.findByBusinessId(query.businessId()).stream()
                .map(
                        membership ->
                                BusinessMembershipListItem.from(
                                        membership, accountSummary(membership.accountId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipAuditHistoryItem> listAuditHistory(
            final MembershipAuditHistoryQuery query) {
        requireActiveOwner(query.actorAccountId(), query.businessId());
        return membershipQueryPort.findAuditEntriesByBusinessId(query.businessId()).stream()
                .map(
                        entry ->
                                MembershipAuditHistoryItem.from(
                                        entry,
                                        accountSummary(entry.actorAccountId()),
                                        accountSummary(entry.targetAccountId())))
                .toList();
    }

    @Override
    public MembershipAdministrationResponse updateRole(final UpdateMembershipRoleCommand command) {
        requireActiveOwner(command.actorAccountId(), command.businessId());
        final var membership = membershipForBusiness(command.membershipId(), command.businessId());
        if (!membership.active()) {
            throw new BusinessMembershipNotFoundException();
        }
        if (membership.role() == command.role()) {
            return response(membership);
        }
        if (membership.role() == BusinessRole.OWNER && command.role() == BusinessRole.STAFF) {
            requireNotLastOwner(command.businessId());
        }
        final var now = clock.instant();
        final var updated = membership.changeRole(command.role(), now);
        membershipCommandPort.save(updated);
        membershipCommandPort.saveAuditEntry(
                auditEntry(
                        updated,
                        command.actorAccountId(),
                        MembershipAuditAction.ROLE_CHANGED,
                        membership.role(),
                        membership.active(),
                        updated.role(),
                        updated.active(),
                        now));
        return response(updated);
    }

    @Override
    public MembershipAdministrationResponse disable(final DisableMembershipCommand command) {
        requireActiveOwner(command.actorAccountId(), command.businessId());
        final var membership = membershipForBusiness(command.membershipId(), command.businessId());
        if (!membership.active()) {
            return response(membership);
        }
        if (membership.role() == BusinessRole.OWNER) {
            requireNotLastOwner(command.businessId());
        }
        final var now = clock.instant();
        final var disabled = membership.disable(now);
        membershipCommandPort.save(disabled);
        membershipCommandPort.saveAuditEntry(
                auditEntry(
                        disabled,
                        command.actorAccountId(),
                        MembershipAuditAction.DISABLED,
                        membership.role(),
                        membership.active(),
                        disabled.role(),
                        disabled.active(),
                        now));
        return response(disabled);
    }

    private void requireActiveOwner(final AccountId actorAccountId, final BusinessId businessId) {
        if (accountQueryPort.findById(actorAccountId).filter(Account::active).isEmpty()) {
            throw new MembershipAdministrationDeniedException();
        }
        if (businessQueryPort.findById(businessId).filter(Business::active).isEmpty()) {
            throw new MembershipAdministrationDeniedException();
        }
        final var membership =
                membershipQueryPort.findActiveByAccountIdAndBusinessId(actorAccountId, businessId);
        if (membership.filter(value -> value.role() == BusinessRole.OWNER).isEmpty()) {
            throw new MembershipAdministrationDeniedException();
        }
    }

    private BusinessMembership membershipForBusiness(
            final UUID membershipId, final BusinessId businessId) {
        final var membership =
                membershipQueryPort
                        .findById(membershipId)
                        .orElseThrow(BusinessMembershipNotFoundException::new);
        if (!membership.businessId().equals(businessId)) {
            throw new BusinessMembershipNotFoundException();
        }
        return membership;
    }

    private void requireNotLastOwner(final BusinessId businessId) {
        if (membershipQueryPort.countActiveByBusinessIdAndRole(businessId, BusinessRole.OWNER)
                <= 1) {
            throw new LastOwnerMembershipException();
        }
    }

    private MembershipAdministrationResponse response(final BusinessMembership membership) {
        return MembershipAdministrationResponse.from(
                membership, accountSummary(membership.accountId()));
    }

    private MembershipAccountSummary accountSummary(final AccountId accountId) {
        return accountQueryPort
                .findById(accountId)
                .map(MembershipAccountSummary::from)
                .orElseThrow(() -> new IllegalStateException("Membership account is missing"));
    }

    private static BusinessMembershipAuditEntry auditEntry(
            final BusinessMembership membership,
            final AccountId actorAccountId,
            final MembershipAuditAction action,
            final BusinessRole previousRole,
            final Boolean previousActive,
            final BusinessRole newRole,
            final Boolean newActive,
            final Instant now) {
        return BusinessMembershipAuditEntry.create(
                membership.id(),
                membership.businessId(),
                actorAccountId,
                membership.accountId(),
                action,
                previousRole,
                previousActive,
                newRole,
                newActive,
                now);
    }
}

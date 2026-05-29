package io.resrv.platform.application.membership.in;

import java.util.List;

public interface BusinessMembershipAdministrationUseCase {

    MembershipAdministrationResponse grantStaff(GrantStaffMembershipCommand command);

    List<BusinessMembershipListItem> listMemberships(ListBusinessMembershipsQuery query);

    List<MembershipAuditHistoryItem> listAuditHistory(MembershipAuditHistoryQuery query);

    MembershipAdministrationResponse updateRole(UpdateMembershipRoleCommand command);

    MembershipAdministrationResponse disable(DisableMembershipCommand command);
}

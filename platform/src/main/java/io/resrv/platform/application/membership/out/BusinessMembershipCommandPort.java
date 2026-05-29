package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessMembershipAuditEntry;

public interface BusinessMembershipCommandPort {

    void save(BusinessMembership membership);

    void saveAuditEntry(BusinessMembershipAuditEntry auditEntry);
}

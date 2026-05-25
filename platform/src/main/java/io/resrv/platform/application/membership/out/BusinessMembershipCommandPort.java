package io.resrv.platform.application.membership.out;

import io.resrv.platform.domain.membership.BusinessMembership;

public interface BusinessMembershipCommandPort {

    void save(BusinessMembership membership);
}

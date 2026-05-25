package io.resrv.timeslot.application.auth.out;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

public interface BusinessAccessPort {

    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}

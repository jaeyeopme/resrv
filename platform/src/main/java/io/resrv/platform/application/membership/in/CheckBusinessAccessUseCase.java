package io.resrv.platform.application.membership.in;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;

public interface CheckBusinessAccessUseCase {

    boolean hasBusinessAccess(AccountId accountId, BusinessId businessId);
}

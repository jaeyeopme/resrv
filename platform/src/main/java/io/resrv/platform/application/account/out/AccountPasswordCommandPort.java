package io.resrv.platform.application.account.out;

import io.resrv.shared.kernel.AccountId;

public interface AccountPasswordCommandPort {

    void updatePasswordHash(AccountId accountId, String hashedPassword);
}

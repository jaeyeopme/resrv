package io.resrv.platform.application.account.out;

import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.shared.kernel.AccountId;
import java.util.Optional;

public interface AccountQueryPort {

    Optional<Account> findById(AccountId accountId);

    Optional<Account> findByEmail(AccountEmail email);
}

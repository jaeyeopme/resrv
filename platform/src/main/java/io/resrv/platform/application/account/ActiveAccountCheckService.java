package io.resrv.platform.application.account;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.exchange.account.ActiveAccountCheck;
import io.resrv.shared.kernel.AccountId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActiveAccountCheckService implements ActiveAccountCheck {

    private final AccountQueryPort accountQueryPort;

    public ActiveAccountCheckService(final AccountQueryPort accountQueryPort) {
        this.accountQueryPort = accountQueryPort;
    }

    @Override
    public boolean isActive(final AccountId accountId) {
        return accountQueryPort.findById(accountId).filter(account -> account.active()).isPresent();
    }
}

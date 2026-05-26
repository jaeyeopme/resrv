package io.resrv.platform.application.account;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import io.resrv.platform.domain.account.AccountStatus;
import io.resrv.shared.kernel.AccountId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveAccountCheckServiceTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");

    private AccountQueryPort accountQueryPort;
    private ActiveAccountCheckService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = mock(AccountQueryPort.class);
        service = new ActiveAccountCheckService(accountQueryPort);
    }

    @Test
    void activeAccountIsActive() {
        when(accountQueryPort.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(account(AccountStatus.ACTIVE)));

        assertTrue(service.isActive(ACCOUNT_ID));
    }

    @Test
    void missingOrDisabledAccountIsNotActive() {
        when(accountQueryPort.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
        assertFalse(service.isActive(ACCOUNT_ID));

        when(accountQueryPort.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(account(AccountStatus.DISABLED)));
        assertFalse(service.isActive(ACCOUNT_ID));
    }

    private static Account account(final AccountStatus status) {
        return Account.reconstitute(
                ACCOUNT_ID,
                new AccountEmail("owner@example.com"),
                new AccountName("Owner"),
                "$argon2id$test",
                status,
                NOW);
    }
}

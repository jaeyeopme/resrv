package io.resrv.platform.adapter.out.persistence.account;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HASHED_PASSWORD = "$argon2id$v=19$m=65536,t=3,p=1$platform";

    @Test
    void save_whenNonEmailConstraintFails_rethrowsPersistenceException() {
        final var repository = mock(AccountJpaRepository.class);
        final var entityManager = mock(EntityManager.class);
        final var adapter = new AccountPersistenceAdapter(repository, entityManager);
        final var account = createAccount();
        when(repository.save(any(AccountJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new PersistenceException("ck_platform_account_hashed_password_not_blank"))
                .when(entityManager)
                .flush();

        assertThrows(PersistenceException.class, () -> adapter.save(account));
    }

    private static Account createAccount() {
        return Account.create(
                new AccountEmail("owner@example.com"),
                new AccountName("Owner"),
                HASHED_PASSWORD,
                NOW);
    }
}

package io.resrv.platform.adapter.out.persistence.account;

import io.resrv.platform.adapter.out.persistence.PersistenceConstraintViolation;
import io.resrv.platform.application.account.out.AccountCommandPort;
import io.resrv.platform.application.account.out.AccountPasswordCommandPort;
import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.AccountEmailAlreadyExistsException;
import io.resrv.shared.kernel.AccountId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class AccountPersistenceAdapter
        implements AccountCommandPort, AccountQueryPort, AccountPasswordCommandPort {

    private static final String UNIQUE_EMAIL = "uq_platform_account_email";

    private final AccountJpaRepository accountJpaRepository;
    private final EntityManager entityManager;

    AccountPersistenceAdapter(
            final AccountJpaRepository accountJpaRepository, final EntityManager entityManager) {
        this.accountJpaRepository = accountJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Account account) {
        try {
            accountJpaRepository.save(AccountJpaEntity.fromDomain(account));
            entityManager.flush();
        } catch (final DataIntegrityViolationException | PersistenceException exception) {
            if (PersistenceConstraintViolation.isCausedBy(exception, UNIQUE_EMAIL)) {
                throw new AccountEmailAlreadyExistsException(account.email());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Account> findById(final AccountId accountId) {
        return accountJpaRepository.findById(accountId.value()).map(AccountJpaEntity::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(final AccountEmail email) {
        return accountJpaRepository.findByEmail(email.value()).map(AccountJpaEntity::toDomain);
    }

    @Override
    public void updatePasswordHash(final AccountId accountId, final String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Account hashed password must not be blank");
        }
        accountJpaRepository.updatePasswordHash(accountId.value(), hashedPassword);
        entityManager.flush();
    }
}

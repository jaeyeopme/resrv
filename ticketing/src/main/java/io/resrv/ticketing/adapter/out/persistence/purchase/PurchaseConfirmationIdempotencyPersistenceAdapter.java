package io.resrv.ticketing.adapter.out.persistence.purchase;

import io.resrv.shared.kernel.AccountId;
import io.resrv.ticketing.application.purchase.out.PurchaseConfirmationIdempotencyPort;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotency;
import io.resrv.ticketing.domain.purchase.PurchaseConfirmationIdempotencyKey;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PurchaseConfirmationIdempotencyPersistenceAdapter
        implements PurchaseConfirmationIdempotencyPort {

    private final PurchaseConfirmationIdempotencyJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    PurchaseConfirmationIdempotencyPersistenceAdapter(
            final PurchaseConfirmationIdempotencyJpaRepository repository,
            final JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PurchaseConfirmationIdempotency> findForCustomerKey(
            final AccountId customerAccountId,
            final PurchaseConfirmationIdempotencyKey idempotencyKey) {
        return repository
                .findByCustomerAccountIdAndIdempotencyKey(
                        customerAccountId.value(), idempotencyKey.value())
                .map(PurchaseConfirmationIdempotencyMapper::toDomain);
    }

    @Override
    public PurchaseConfirmationIdempotency createPendingOrFindExisting(
            final PurchaseConfirmationIdempotency pendingIdempotency) {
        lockCustomerKey(pendingIdempotency);
        final var existing =
                findForCustomerKey(
                        pendingIdempotency.customerAccountId(),
                        pendingIdempotency.idempotencyKey());
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        try {
            return PurchaseConfirmationIdempotencyMapper.toDomain(
                    repository.saveAndFlush(
                            PurchaseConfirmationIdempotencyMapper.toEntity(pendingIdempotency)));
        } catch (final DataIntegrityViolationException exception) {
            return findForCustomerKey(
                            pendingIdempotency.customerAccountId(),
                            pendingIdempotency.idempotencyKey())
                    .orElseThrow(() -> exception);
        }
    }

    private void lockCustomerKey(final PurchaseConfirmationIdempotency idempotency) {
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                ps ->
                        ps.setString(
                                1,
                                idempotency.customerAccountId().value()
                                        + ":"
                                        + idempotency.idempotencyKey().value()),
                rs -> null);
    }

    @Override
    public PurchaseConfirmationIdempotency save(final PurchaseConfirmationIdempotency idempotency) {
        return PurchaseConfirmationIdempotencyMapper.toDomain(
                repository.save(PurchaseConfirmationIdempotencyMapper.toEntity(idempotency)));
    }
}

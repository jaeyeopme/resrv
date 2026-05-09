package io.resrv.adapter.out.persistence.customer;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.application.customer.out.CustomerCommandPort;
import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.tenant.TenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class CustomerPersistenceAdapter implements CustomerCommandPort, CustomerQueryPort {

    private final CustomerJpaRepository customerJpaRepository;
    private final EntityManager entityManager;

    CustomerPersistenceAdapter(
            final CustomerJpaRepository customerJpaRepository, final EntityManager entityManager) {
        this.customerJpaRepository = customerJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(final Customer customer) {
        try {
            customerJpaRepository.save(CustomerPersistenceMapper.toJpaEntity(customer));
            entityManager.flush();
        } catch (final DataIntegrityViolationException | PersistenceException _) {
            throw new CustomerEmailAlreadyExistsException(customer.tenantId(), customer.email());
        }
    }

    @Override
    public boolean existsByTenantIdAndEmail(final TenantId tenantId, final CustomerEmail email) {
        return customerJpaRepository.existsByTenantIdAndEmail(tenantId.value(), email.value());
    }

    @Override
    public Optional<UserCredentials> findCredentialsByTenantIdAndEmail(
            final TenantId tenantId, final String email) {
        return customerJpaRepository
                .findByTenantIdAndEmail(tenantId.value(), email)
                .map(
                        entity ->
                                new UserCredentials(
                                        entity.getId(),
                                        entity.getTenantId(),
                                        entity.getHashedPassword(),
                                        RoleNames.CUSTOMER,
                                        entity.isActive()));
    }

    @Override
    public Optional<Customer> findByTenantIdAndId(
            final TenantId tenantId, final CustomerId customerId) {
        return customerJpaRepository
                .findByTenantIdAndId(tenantId.value(), customerId.value())
                .map(CustomerPersistenceMapper::toDomain);
    }
}

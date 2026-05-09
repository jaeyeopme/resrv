package io.resrv.adapter.out.persistence.customer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface CustomerJpaRepository extends CrudRepository<CustomerJpaEntity, UUID> {

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    Optional<CustomerJpaEntity> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<CustomerJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}

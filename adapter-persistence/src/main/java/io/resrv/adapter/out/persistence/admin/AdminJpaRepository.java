package io.resrv.adapter.out.persistence.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface AdminJpaRepository extends CrudRepository<AdminJpaEntity, UUID> {

    Optional<AdminJpaEntity> findByTenantIdAndEmail(UUID tenantId, String email);
}

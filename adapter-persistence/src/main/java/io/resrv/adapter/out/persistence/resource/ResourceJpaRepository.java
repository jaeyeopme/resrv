package io.resrv.adapter.out.persistence.resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface ResourceJpaRepository extends CrudRepository<ResourceJpaEntity, UUID> {

    Optional<ResourceJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<ResourceJpaEntity> findByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    List<ResourceJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}

package io.resrv.adapter.out.persistence.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface TenantJpaRepository extends CrudRepository<TenantJpaEntity, UUID> {

    boolean existsBySlug(String slug);

    @Query("SELECT t.id FROM TenantJpaEntity t WHERE t.slug = :slug")
    Optional<UUID> findIdBySlug(String slug);

    Optional<TenantJpaEntity> findBySlug(String slug);
}

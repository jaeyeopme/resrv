package io.resrv.platform.adapter.out.persistence.business;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface BusinessJpaRepository extends CrudRepository<BusinessJpaEntity, UUID> {

    Optional<BusinessJpaEntity> findBySlug(String slug);
}

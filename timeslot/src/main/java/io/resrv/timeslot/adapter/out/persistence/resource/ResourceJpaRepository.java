package io.resrv.timeslot.adapter.out.persistence.resource;

import io.resrv.timeslot.domain.resource.ResourceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface ResourceJpaRepository extends CrudRepository<ResourceJpaEntity, UUID> {

    Optional<ResourceJpaEntity> findByBusinessIdAndId(UUID businessId, UUID id);

    List<ResourceJpaEntity> findByBusinessIdAndStatus(UUID businessId, ResourceStatus status);
}

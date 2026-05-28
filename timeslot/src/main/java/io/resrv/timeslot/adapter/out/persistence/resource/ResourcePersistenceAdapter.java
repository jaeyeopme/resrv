package io.resrv.timeslot.adapter.out.persistence.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import io.resrv.timeslot.domain.resource.ResourceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ResourcePersistenceAdapter implements ResourceCommandPort, ResourceQueryPort {

    private final ResourceJpaRepository repository;

    ResourcePersistenceAdapter(final ResourceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(final Resource resource) {
        repository.save(ResourceJpaEntity.fromDomain(resource));
    }

    @Override
    public Optional<Resource> findByBusinessIdAndSlug(
            final BusinessId businessId, final ResourceSlug slug) {
        return repository
                .findByBusinessIdAndSlug(businessId.value(), slug.value())
                .map(ResourceJpaEntity::toDomain);
    }

    @Override
    public Optional<Resource> findById(final ResourceId resourceId) {
        return repository.findById(resourceId.value()).map(ResourceJpaEntity::toDomain);
    }

    @Override
    public Optional<Resource> findByBusinessIdAndId(
            final BusinessId businessId, final ResourceId resourceId) {
        return repository
                .findByBusinessIdAndId(businessId.value(), resourceId.value())
                .map(ResourceJpaEntity::toDomain);
    }

    @Override
    public List<Resource> findActiveByBusinessId(final BusinessId businessId) {
        return repository
                .findByBusinessIdAndStatus(businessId.value(), ResourceStatus.ACTIVE)
                .stream()
                .map(ResourceJpaEntity::toDomain)
                .toList();
    }
}

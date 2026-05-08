package io.resrv.application.resource;

import io.resrv.application.resource.in.CreateResourceCommand;
import io.resrv.application.resource.in.CreateResourceUseCase;
import io.resrv.application.resource.in.DeactivateResourceCommand;
import io.resrv.application.resource.in.DeactivateResourceUseCase;
import io.resrv.application.resource.in.GetResourceQuery;
import io.resrv.application.resource.in.GetResourceUseCase;
import io.resrv.application.resource.in.ListResourcesQuery;
import io.resrv.application.resource.in.ListResourcesUseCase;
import io.resrv.application.resource.in.ResourceResult;
import io.resrv.application.resource.in.UpdateResourceCommand;
import io.resrv.application.resource.in.UpdateResourceUseCase;
import io.resrv.application.resource.out.ResourceCommandPort;
import io.resrv.application.resource.out.ResourceQueryPort;
import io.resrv.domain.resource.Resource;
import io.resrv.domain.resource.ResourceDescription;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceName;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceSlugAlreadyExistsException;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ResourceService
        implements CreateResourceUseCase,
                GetResourceUseCase,
                ListResourcesUseCase,
                UpdateResourceUseCase,
                DeactivateResourceUseCase {

    private final Clock clock;
    private final ResourceCommandPort resourceCommandPort;
    private final ResourceQueryPort resourceQueryPort;

    ResourceService(
            final Clock clock,
            final ResourceCommandPort resourceCommandPort,
            final ResourceQueryPort resourceQueryPort) {
        this.clock = clock;
        this.resourceCommandPort = resourceCommandPort;
        this.resourceQueryPort = resourceQueryPort;
    }

    @Override
    public ResourceResult create(final CreateResourceCommand command) {
        final var slug = new ResourceSlug(command.slug());
        if (resourceQueryPort.existsByTenantIdAndSlug(command.tenantId(), slug)) {
            throw new ResourceSlugAlreadyExistsException(command.tenantId(), slug);
        }

        final var now = clock.instant();
        final var resource =
                Resource.create(
                        command.tenantId(),
                        new ResourceName(command.name()),
                        slug,
                        new ResourceDescription(command.description()),
                        now);

        resourceCommandPort.save(resource);
        return ResourceResult.from(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResult get(final GetResourceQuery query) {
        return ResourceResult.from(findResource(query.tenantId(), query.resourceId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResult> list(final ListResourcesQuery query) {
        return resourceQueryPort
                .findByTenantIdAndStatus(query.tenantId(), ResourceStatus.ACTIVE)
                .stream()
                .map(ResourceResult::from)
                .toList();
    }

    @Override
    public ResourceResult update(final UpdateResourceCommand command) {
        final var resource = findResource(command.tenantId(), command.resourceId());
        final var newSlug = new ResourceSlug(command.slug());
        if (!resource.slug().equals(newSlug)
                && resourceQueryPort.existsByTenantIdAndSlug(command.tenantId(), newSlug)) {
            throw new ResourceSlugAlreadyExistsException(command.tenantId(), newSlug);
        }

        final var now = clock.instant();
        final var updated =
                resource.rename(new ResourceName(command.name()), now)
                        .changeSlug(newSlug, now)
                        .changeDescription(new ResourceDescription(command.description()), now);

        resourceCommandPort.save(updated);
        return ResourceResult.from(updated);
    }

    @Override
    public void deactivate(final DeactivateResourceCommand command) {
        final var resource = findResource(command.tenantId(), command.resourceId());
        resourceCommandPort.save(resource.deactivate(clock.instant()));
    }

    private Resource findResource(final TenantId tenantId, final ResourceId resourceId) {
        return resourceQueryPort
                .findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(tenantId, resourceId));
    }
}

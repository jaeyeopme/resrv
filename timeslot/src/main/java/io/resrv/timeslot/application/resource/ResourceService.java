package io.resrv.timeslot.application.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.business.BusinessNotAvailableException;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import io.resrv.timeslot.application.resource.in.ActivateResourceCommand;
import io.resrv.timeslot.application.resource.in.ActivateResourceUseCase;
import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.in.CreateResourceUseCase;
import io.resrv.timeslot.application.resource.in.DeactivateResourceCommand;
import io.resrv.timeslot.application.resource.in.DeactivateResourceUseCase;
import io.resrv.timeslot.application.resource.in.ListResourcesUseCase;
import io.resrv.timeslot.application.resource.in.ReplaceResourceDetailsCommand;
import io.resrv.timeslot.application.resource.in.ReplaceResourceDetailsUseCase;
import io.resrv.timeslot.application.resource.in.ResourceResult;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.settings.CancellationWindow;
import io.resrv.timeslot.domain.settings.HoldTtl;
import io.resrv.timeslot.domain.settings.SlotDuration;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceService
        implements CreateResourceUseCase,
                ListResourcesUseCase,
                ReplaceResourceDetailsUseCase,
                ActivateResourceUseCase,
                DeactivateResourceUseCase {

    private final BusinessBookingSettingsQueryPort settingsQueryPort;
    private final BusinessLookupPort businessLookupPort;
    private final ResourceCommandPort commandPort;
    private final ResourceQueryPort queryPort;
    private final Clock clock;

    public ResourceService(
            final BusinessBookingSettingsQueryPort settingsQueryPort,
            final BusinessLookupPort businessLookupPort,
            final ResourceCommandPort commandPort,
            final ResourceQueryPort queryPort,
            final Clock clock) {
        this.settingsQueryPort = settingsQueryPort;
        this.businessLookupPort = businessLookupPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    public ResourceResult create(final CreateResourceCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var name = new ResourceName(command.name());
        final var description = Resource.normalizeDescription(command.description());
        final var overrides =
                overrides(
                        command.slotDurationMinutes(),
                        command.holdTtlMinutes(),
                        command.cancellationWindowMinutes());

        if (settingsQueryPort.findByBusinessId(command.businessId()).isEmpty()) {
            throw new BookingSettingsRequiredException(command.businessId());
        }

        final var now = clock.instant();
        final var resource =
                Resource.create(command.businessId(), name, description, overrides, now);
        commandPort.save(resource);
        return ResourceResult.from(resource);
    }

    @Override
    public ResourceResult replaceDetails(final ReplaceResourceDetailsCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var name = new ResourceName(command.name());
        final var description = Resource.normalizeDescription(command.description());
        final var overrides =
                overrides(
                        command.slotDurationMinutes(),
                        command.holdTtlMinutes(),
                        command.cancellationWindowMinutes());

        final var resource = loadResource(command.businessId(), command.resourceId());

        final var updated = resource.replaceDetails(name, description, overrides, clock.instant());
        commandPort.save(updated);
        return ResourceResult.from(updated);
    }

    @Override
    public ResourceResult activate(final ActivateResourceCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var resource = loadResource(command.businessId(), command.resourceId());
        final var updated = resource.activate(clock.instant());
        commandPort.save(updated);
        return ResourceResult.from(updated);
    }

    @Override
    public ResourceResult deactivate(final DeactivateResourceCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var resource = loadResource(command.businessId(), command.resourceId());
        final var updated = resource.deactivate(clock.instant());
        commandPort.save(updated);
        return ResourceResult.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResult> listActive(final BusinessId businessId) {
        Objects.requireNonNull(businessId, "Business id must not be null");
        if (businessLookupPort.findActiveById(businessId).isEmpty()) {
            return List.of();
        }
        return queryPort.findActiveByBusinessId(businessId).stream()
                .map(ResourceResult::from)
                .toList();
    }

    private Resource loadResource(final BusinessId businessId, final ResourceId resourceId) {
        ensureBusinessActive(businessId);
        return queryPort
                .findByBusinessIdAndId(businessId, resourceId)
                .orElseThrow(() -> new ResourceNotAvailableException(businessId, resourceId));
    }

    private void ensureBusinessActive(final BusinessId businessId) {
        if (businessLookupPort.findActiveById(businessId).isEmpty()) {
            throw new BusinessNotAvailableException(businessId);
        }
    }

    private static ResourceBookingOverrides overrides(
            final Integer slotDurationMinutes,
            final Integer holdTtlMinutes,
            final Integer cancellationWindowMinutes) {
        return new ResourceBookingOverrides(
                slotDurationMinutes == null ? null : new SlotDuration(slotDurationMinutes),
                holdTtlMinutes == null ? null : new HoldTtl(holdTtlMinutes),
                cancellationWindowMinutes == null
                        ? null
                        : new CancellationWindow(cancellationWindowMinutes));
    }
}

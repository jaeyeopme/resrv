package io.resrv.timeslot.application.resource;

import io.resrv.timeslot.application.resource.in.CreateResourceCommand;
import io.resrv.timeslot.application.resource.in.CreateResourceUseCase;
import io.resrv.timeslot.application.resource.in.ListResourcesUseCase;
import io.resrv.timeslot.application.resource.in.ResourceResult;
import io.resrv.timeslot.application.resource.out.ResourceCommandPort;
import io.resrv.timeslot.application.resource.out.ResourceQueryPort;
import io.resrv.timeslot.application.settings.BookingSettingsRequiredException;
import io.resrv.timeslot.application.settings.out.BusinessBookingSettingsQueryPort;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceBookingOverrides;
import io.resrv.timeslot.domain.resource.ResourceName;
import io.resrv.timeslot.domain.resource.ResourceSlug;
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
public class ResourceService implements CreateResourceUseCase, ListResourcesUseCase {

    private final BusinessBookingSettingsQueryPort settingsQueryPort;
    private final ResourceCommandPort commandPort;
    private final ResourceQueryPort queryPort;
    private final Clock clock;

    public ResourceService(
            final BusinessBookingSettingsQueryPort settingsQueryPort,
            final ResourceCommandPort commandPort,
            final ResourceQueryPort queryPort,
            final Clock clock) {
        this.settingsQueryPort = settingsQueryPort;
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    public ResourceResult create(final CreateResourceCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        final var name = new ResourceName(command.name());
        final var slug = new ResourceSlug(command.slug());
        final var description = Resource.normalizeDescription(command.description());
        final var overrides =
                new ResourceBookingOverrides(
                        command.slotDurationMinutes() == null
                                ? null
                                : new SlotDuration(command.slotDurationMinutes()),
                        command.holdTtlMinutes() == null
                                ? null
                                : new HoldTtl(command.holdTtlMinutes()),
                        command.cancellationWindowMinutes() == null
                                ? null
                                : new CancellationWindow(command.cancellationWindowMinutes()));

        if (settingsQueryPort.findByBusinessId(command.businessId()).isEmpty()) {
            throw new BookingSettingsRequiredException(command.businessId());
        }
        if (queryPort.findByBusinessIdAndSlug(command.businessId(), slug).isPresent()) {
            throw new ResourceSlugAlreadyExistsException(command.businessId(), slug);
        }

        final var now = clock.instant();
        final var resource =
                Resource.create(command.businessId(), name, slug, description, overrides, now);
        commandPort.save(resource);
        return ResourceResult.from(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResult> listActive(final io.resrv.shared.kernel.BusinessId businessId) {
        Objects.requireNonNull(businessId, "Business id must not be null");
        return queryPort.findActiveByBusinessId(businessId).stream()
                .map(ResourceResult::from)
                .toList();
    }
}

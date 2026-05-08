package io.resrv.application.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.application.resource.in.CreateResourceCommand;
import io.resrv.application.resource.in.DeactivateResourceCommand;
import io.resrv.application.resource.in.GetResourceQuery;
import io.resrv.application.resource.in.ListResourcesQuery;
import io.resrv.application.resource.in.UpdateResourceCommand;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResourceServiceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");

    private ResourceCommandPort commandPort;
    private ResourceQueryPort queryPort;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(ResourceCommandPort.class);
        queryPort = mock(ResourceQueryPort.class);
        service =
                new ResourceService(Clock.fixed(FIXED_NOW, ZoneOffset.UTC), commandPort, queryPort);
    }

    @Test
    void create_savesActiveResourceForTenant() {
        when(queryPort.existsByTenantIdAndSlug(TENANT_ID, new ResourceSlug("room-a")))
                .thenReturn(false);

        final var result =
                service.create(new CreateResourceCommand(TENANT_ID, "Room A", "room-a", "Quiet"));

        assertEquals("Room A", result.name());
        assertEquals("room-a", result.slug());
        assertEquals("Quiet", result.description());
        assertEquals(ResourceStatus.ACTIVE, result.status());
        assertEquals(FIXED_NOW, result.createdAt());

        final var captor = ArgumentCaptor.forClass(Resource.class);
        verify(commandPort).save(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().tenantId());
    }

    @Test
    void create_duplicateSlug_throws() {
        when(queryPort.existsByTenantIdAndSlug(TENANT_ID, new ResourceSlug("room-a")))
                .thenReturn(true);

        assertThrows(
                ResourceSlugAlreadyExistsException.class,
                () ->
                        service.create(
                                new CreateResourceCommand(TENANT_ID, "Room A", "room-a", null)));
        verify(commandPort, never()).save(any());
    }

    @Test
    void get_returnsResourceWhenSameTenant() {
        final var resource = createResource("room-a");
        when(queryPort.findByTenantIdAndId(TENANT_ID, resource.id()))
                .thenReturn(Optional.of(resource));

        final var result = service.get(new GetResourceQuery(TENANT_ID, resource.id()));

        assertEquals(resource.id().value(), result.id());
        assertEquals("room-a", result.slug());
    }

    @Test
    void get_missingResource_throws() {
        final var resourceId = ResourceId.create();
        when(queryPort.findByTenantIdAndId(TENANT_ID, resourceId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.get(new GetResourceQuery(TENANT_ID, resourceId)));
    }

    @Test
    void list_returnsActiveResourcesOnly() {
        when(queryPort.findByTenantIdAndStatus(TENANT_ID, ResourceStatus.ACTIVE))
                .thenReturn(List.of(createResource("room-a")));

        final var results = service.list(new ListResourcesQuery(TENANT_ID));

        assertEquals(1, results.size());
        assertEquals("room-a", results.getFirst().slug());
        verify(queryPort).findByTenantIdAndStatus(TENANT_ID, ResourceStatus.ACTIVE);
    }

    @Test
    void update_checksTenantScopedSlugConflictBeforeSaving() {
        final var resource = createResource("room-a");
        when(queryPort.findByTenantIdAndId(TENANT_ID, resource.id()))
                .thenReturn(Optional.of(resource));
        when(queryPort.existsByTenantIdAndSlug(TENANT_ID, new ResourceSlug("room-b")))
                .thenReturn(false);

        final var result =
                service.update(
                        new UpdateResourceCommand(
                                TENANT_ID, resource.id(), "Room B", "room-b", "Updated"));

        assertEquals("Room B", result.name());
        assertEquals("room-b", result.slug());
        assertEquals("Updated", result.description());
        verify(commandPort).save(any(Resource.class));
    }

    @Test
    void update_sameSlug_doesNotPerformDuplicateCheck() {
        final var resource = createResource("room-a");
        when(queryPort.findByTenantIdAndId(TENANT_ID, resource.id()))
                .thenReturn(Optional.of(resource));

        service.update(
                new UpdateResourceCommand(TENANT_ID, resource.id(), "Room A+", "room-a", null));

        verify(queryPort, never()).existsByTenantIdAndSlug(any(), any());
    }

    @Test
    void update_duplicateNewSlug_throws() {
        final var resource = createResource("room-a");
        when(queryPort.findByTenantIdAndId(TENANT_ID, resource.id()))
                .thenReturn(Optional.of(resource));
        when(queryPort.existsByTenantIdAndSlug(TENANT_ID, new ResourceSlug("room-b")))
                .thenReturn(true);

        assertThrows(
                ResourceSlugAlreadyExistsException.class,
                () ->
                        service.update(
                                new UpdateResourceCommand(
                                        TENANT_ID, resource.id(), "Room B", "room-b", null)));
        verify(commandPort, never()).save(any());
    }

    @Test
    void deactivate_marksResourceInactive() {
        final var resource = createResource("room-a");
        when(queryPort.findByTenantIdAndId(TENANT_ID, resource.id()))
                .thenReturn(Optional.of(resource));

        service.deactivate(new DeactivateResourceCommand(TENANT_ID, resource.id()));

        final var captor = ArgumentCaptor.forClass(Resource.class);
        verify(commandPort).save(captor.capture());
        assertEquals(ResourceStatus.INACTIVE, captor.getValue().status());
        assertTrue(captor.getValue().updatedAt().equals(FIXED_NOW));
    }

    private static Resource createResource(final String slug) {
        return Resource.create(
                TENANT_ID,
                new ResourceName("Room A"),
                new ResourceSlug(slug),
                new ResourceDescription("Quiet"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }
}

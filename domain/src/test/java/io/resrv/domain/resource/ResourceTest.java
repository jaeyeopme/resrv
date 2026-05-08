package io.resrv.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResourceTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2025-01-02T00:00:00Z");

    @Test
    void create_defaultsToActiveAndSameTimestamps() {
        final var resource = createResource();

        assertNotNull(resource.id());
        assertEquals(TENANT_ID, resource.tenantId());
        assertEquals(ResourceStatus.ACTIVE, resource.status());
        assertEquals(CREATED_AT, resource.createdAt());
        assertEquals(CREATED_AT, resource.updatedAt());
    }

    @Test
    void updateOperations_preserveIdentityAndCreationTime() {
        final var resource =
                createResource()
                        .rename(new ResourceName("Room B"), UPDATED_AT)
                        .changeSlug(new ResourceSlug("room-b"), UPDATED_AT)
                        .changeDescription(new ResourceDescription("Updated"), UPDATED_AT);

        assertEquals("Room B", resource.name().value());
        assertEquals("room-b", resource.slug().value());
        assertEquals("Updated", resource.description().value());
        assertEquals(CREATED_AT, resource.createdAt());
        assertEquals(UPDATED_AT, resource.updatedAt());
    }

    @Test
    void deactivate_marksInactiveAndIsIdempotent() {
        final var inactive = createResource().deactivate(UPDATED_AT);

        assertEquals(ResourceStatus.INACTIVE, inactive.status());
        assertEquals(UPDATED_AT, inactive.updatedAt());
        assertSame(inactive, inactive.deactivate(Instant.parse("2025-01-03T00:00:00Z")));
    }

    @Test
    void equalsAndHashCode_useIdentityOnly() {
        final var id = ResourceId.create();
        final var resource =
                Resource.reconstitute(
                        id,
                        TENANT_ID,
                        new ResourceName("Room A"),
                        new ResourceSlug("room-a"),
                        new ResourceDescription("Quiet"),
                        ResourceStatus.ACTIVE,
                        CREATED_AT,
                        CREATED_AT);
        final var sameIdentityWithDifferentState =
                Resource.reconstitute(
                        id,
                        TENANT_ID,
                        new ResourceName("Room B"),
                        new ResourceSlug("room-b"),
                        ResourceDescription.empty(),
                        ResourceStatus.INACTIVE,
                        CREATED_AT,
                        UPDATED_AT);
        final var other = createResource();

        assertEquals(resource, sameIdentityWithDifferentState);
        assertEquals(resource.hashCode(), sameIdentityWithDifferentState.hashCode());
        assertNotEquals(resource, other);
        assertNotEquals(resource, "resource");
    }

    @Test
    void reconstitute_rejectsNullRequiredValues() {
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                null,
                                TENANT_ID,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                null,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                null,
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                new ResourceName("Room A"),
                                null,
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                null,
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                null,
                                CREATED_AT,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                null,
                                CREATED_AT));
        assertThrows(
                NullPointerException.class,
                () ->
                        Resource.reconstitute(
                                ResourceId.create(),
                                TENANT_ID,
                                new ResourceName("Room A"),
                                new ResourceSlug("room-a"),
                                ResourceDescription.empty(),
                                ResourceStatus.ACTIVE,
                                CREATED_AT,
                                null));
    }

    private static Resource createResource() {
        return Resource.create(
                TENANT_ID,
                new ResourceName("Room A"),
                new ResourceSlug("room-a"),
                new ResourceDescription("Quiet"),
                CREATED_AT);
    }
}

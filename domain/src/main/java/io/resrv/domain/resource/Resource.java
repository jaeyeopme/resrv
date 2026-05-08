package io.resrv.domain.resource;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;

public final class Resource {

    private final ResourceId id;
    private final TenantId tenantId;
    private final ResourceName name;
    private final ResourceSlug slug;
    private final ResourceDescription description;
    private final ResourceStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Resource(
            final ResourceId id,
            final TenantId tenantId,
            final ResourceName name,
            final ResourceSlug slug,
            final ResourceDescription description,
            final ResourceStatus status,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Resource id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Resource tenant id must not be null");
        this.name = Objects.requireNonNull(name, "Resource name must not be null");
        this.slug = Objects.requireNonNull(slug, "Resource slug must not be null");
        this.description =
                Objects.requireNonNull(description, "Resource description must not be null");
        this.status = Objects.requireNonNull(status, "Resource status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Resource createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Resource updatedAt must not be null");
    }

    public static Resource create(
            final TenantId tenantId,
            final ResourceName name,
            final ResourceSlug slug,
            final ResourceDescription description,
            final Instant now) {
        return new Resource(
                ResourceId.create(),
                tenantId,
                name,
                slug,
                description,
                ResourceStatus.ACTIVE,
                now,
                now);
    }

    public static Resource reconstitute(
            final ResourceId id,
            final TenantId tenantId,
            final ResourceName name,
            final ResourceSlug slug,
            final ResourceDescription description,
            final ResourceStatus status,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Resource(id, tenantId, name, slug, description, status, createdAt, updatedAt);
    }

    public Resource rename(final ResourceName newName, final Instant now) {
        return new Resource(id, tenantId, newName, slug, description, status, createdAt, now);
    }

    public Resource changeSlug(final ResourceSlug newSlug, final Instant now) {
        return new Resource(id, tenantId, name, newSlug, description, status, createdAt, now);
    }

    public Resource changeDescription(final ResourceDescription newDescription, final Instant now) {
        return new Resource(id, tenantId, name, slug, newDescription, status, createdAt, now);
    }

    public Resource deactivate(final Instant now) {
        if (status == ResourceStatus.INACTIVE) {
            return this;
        }
        return new Resource(
                id, tenantId, name, slug, description, ResourceStatus.INACTIVE, createdAt, now);
    }

    public ResourceId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ResourceName name() {
        return name;
    }

    public ResourceSlug slug() {
        return slug;
    }

    public ResourceDescription description() {
        return description;
    }

    public ResourceStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof Resource other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

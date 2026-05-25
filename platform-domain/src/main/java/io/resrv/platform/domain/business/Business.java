package io.resrv.platform.domain.business;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Objects;

public final class Business {

    private final BusinessId id;
    private final BusinessName name;
    private final BusinessSlug slug;
    private final Timezone timezone;
    private final BusinessStatus status;
    private final Instant createdAt;

    private Business(
            final BusinessId id,
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final BusinessStatus status,
            final Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Business id must not be null");
        this.name = Objects.requireNonNull(name, "Business name must not be null");
        this.slug = Objects.requireNonNull(slug, "Business slug must not be null");
        this.timezone = Objects.requireNonNull(timezone, "Business timezone must not be null");
        this.status = Objects.requireNonNull(status, "Business status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Business createdAt must not be null");
    }

    public static Business create(
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final Instant now) {
        return new Business(BusinessId.create(), name, slug, timezone, BusinessStatus.ACTIVE, now);
    }

    public static Business reconstitute(
            final BusinessId id,
            final BusinessName name,
            final BusinessSlug slug,
            final Timezone timezone,
            final BusinessStatus status,
            final Instant createdAt) {
        return new Business(id, name, slug, timezone, status, createdAt);
    }

    public BusinessId id() {
        return id;
    }

    public BusinessName name() {
        return name;
    }

    public BusinessSlug slug() {
        return slug;
    }

    public Timezone timezone() {
        return timezone;
    }

    public BusinessStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean active() {
        return status == BusinessStatus.ACTIVE;
    }
}

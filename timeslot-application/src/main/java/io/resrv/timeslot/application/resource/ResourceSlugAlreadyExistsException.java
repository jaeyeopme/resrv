package io.resrv.timeslot.application.resource;

import io.resrv.timeslot.domain.resource.ResourceSlug;

public final class ResourceSlugAlreadyExistsException extends RuntimeException {

    private final ResourceSlug slug;

    public ResourceSlugAlreadyExistsException(final ResourceSlug slug) {
        super("Resource slug already exists: " + slug.value());
        this.slug = slug;
    }

    public ResourceSlug slug() {
        return slug;
    }
}

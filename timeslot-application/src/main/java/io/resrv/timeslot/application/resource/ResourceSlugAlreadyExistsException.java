package io.resrv.timeslot.application.resource;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import java.util.Objects;

public final class ResourceSlugAlreadyExistsException extends RuntimeException {

    private final BusinessId businessId;
    private final ResourceSlug slug;

    public ResourceSlugAlreadyExistsException(
            final BusinessId businessId, final ResourceSlug slug) {
        super(
                "Resource slug already exists for business "
                        + businessId.value()
                        + ": "
                        + slug.value());
        this.businessId = Objects.requireNonNull(businessId, "Business id must not be null");
        this.slug = Objects.requireNonNull(slug, "Resource slug must not be null");
    }

    public BusinessId businessId() {
        return businessId;
    }

    public ResourceSlug slug() {
        return slug;
    }
}

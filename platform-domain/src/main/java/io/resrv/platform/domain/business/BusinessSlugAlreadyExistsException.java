package io.resrv.platform.domain.business;

public final class BusinessSlugAlreadyExistsException extends RuntimeException {

    public BusinessSlugAlreadyExistsException(final BusinessSlug slug) {
        super("Business slug already exists: " + slug.value());
    }
}

package io.resrv.domain.tenant;

public final class SlugAlreadyExistsException extends RuntimeException {

    private final transient Slug slug;

    public SlugAlreadyExistsException(final Slug slug) {
        super("Slug '%s' is already in use".formatted(slug.value()));
        this.slug = slug;
    }

    public Slug slug() {
        return slug;
    }
}

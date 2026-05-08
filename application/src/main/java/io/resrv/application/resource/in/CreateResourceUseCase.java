package io.resrv.application.resource.in;

public interface CreateResourceUseCase {

    ResourceResult create(final CreateResourceCommand command);
}

package io.resrv.application.resource.in;

public interface GetResourceUseCase {

    ResourceResult get(final GetResourceQuery query);
}

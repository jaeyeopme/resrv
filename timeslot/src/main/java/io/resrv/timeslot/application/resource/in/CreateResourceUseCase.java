package io.resrv.timeslot.application.resource.in;

public interface CreateResourceUseCase {

    ResourceResult create(CreateResourceCommand command);
}

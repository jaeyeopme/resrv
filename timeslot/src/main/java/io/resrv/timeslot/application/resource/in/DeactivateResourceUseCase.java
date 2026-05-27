package io.resrv.timeslot.application.resource.in;

public interface DeactivateResourceUseCase {

    ResourceResult deactivate(DeactivateResourceCommand command);
}

package io.resrv.timeslot.application.resource.out;

import io.resrv.timeslot.domain.resource.Resource;

public interface ResourceCommandPort {

    void save(Resource resource);
}

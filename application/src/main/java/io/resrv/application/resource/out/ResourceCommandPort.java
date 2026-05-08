package io.resrv.application.resource.out;

import io.resrv.domain.resource.Resource;

public interface ResourceCommandPort {

    void save(final Resource resource);
}

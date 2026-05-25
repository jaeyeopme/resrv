package io.resrv.timeslot.application.resource.out;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.resource.Resource;
import io.resrv.timeslot.domain.resource.ResourceSlug;
import java.util.Optional;

public interface ResourceQueryPort {

    Optional<Resource> findByBusinessIdAndSlug(BusinessId businessId, ResourceSlug slug);

    Optional<Resource> findById(ResourceId resourceId);
}

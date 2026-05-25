package io.resrv.timeslot.application.resource.in;

import io.resrv.shared.kernel.BusinessId;
import java.util.List;

public interface ListResourcesUseCase {

    List<ResourceResult> listActive(BusinessId businessId);
}

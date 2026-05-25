package io.resrv.platform.contract.business;

import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface ActiveBusinessLookup {

    Optional<ActiveBusinessView> findActiveById(BusinessId businessId);
}

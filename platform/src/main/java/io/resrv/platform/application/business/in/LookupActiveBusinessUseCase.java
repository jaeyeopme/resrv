package io.resrv.platform.application.business.in;

import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface LookupActiveBusinessUseCase {

    Optional<LookupActiveBusinessResult> findActiveById(BusinessId businessId);
}

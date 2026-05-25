package io.resrv.platform.application.business.out;

import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;

public interface BusinessQueryPort {

    Optional<Business> findById(BusinessId businessId);

    Optional<Business> findBySlug(BusinessSlug slug);
}

package io.resrv.platform.application.business;

import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.contract.business.ActiveBusinessLookup;
import io.resrv.platform.contract.business.ActiveBusinessView;
import io.resrv.platform.domain.business.Business;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LookupActiveBusinessService implements ActiveBusinessLookup {

    private final BusinessQueryPort businessQueryPort;

    public LookupActiveBusinessService(final BusinessQueryPort businessQueryPort) {
        this.businessQueryPort = businessQueryPort;
    }

    @Override
    public Optional<ActiveBusinessView> findActiveById(final BusinessId businessId) {
        return businessQueryPort
                .findById(businessId)
                .filter(Business::active)
                .map(
                        business ->
                                new ActiveBusinessView(
                                        business.id(),
                                        business.name().value(),
                                        business.slug().value(),
                                        business.timezone()));
    }
}

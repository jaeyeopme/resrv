package io.resrv.platform.application.business;

import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.exchange.business.BusinessSummaryLookup;
import io.resrv.platform.exchange.business.BusinessSummaryView;
import io.resrv.shared.kernel.BusinessId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LookupBusinessSummaryService implements BusinessSummaryLookup {

    private final BusinessQueryPort businessQueryPort;

    public LookupBusinessSummaryService(final BusinessQueryPort businessQueryPort) {
        this.businessQueryPort = businessQueryPort;
    }

    @Override
    public Optional<BusinessSummaryView> findCurrentSummaryById(final BusinessId businessId) {
        return businessQueryPort.findById(businessId).map(LookupBusinessSummaryService::toView);
    }

    @Override
    public Optional<BusinessSummaryView> findCurrentSummaryBySlug(final String slug) {
        return businessQueryPort
                .findBySlug(new BusinessSlug(slug))
                .map(LookupBusinessSummaryService::toView);
    }

    private static BusinessSummaryView toView(final Business business) {
        return new BusinessSummaryView(
                business.id(),
                business.name().value(),
                business.slug().value(),
                business.timezone());
    }
}

package io.resrv.timeslot.api.support;

import io.resrv.platform.exchange.business.ActiveBusinessLookup;
import io.resrv.platform.exchange.business.ActiveBusinessView;
import io.resrv.platform.exchange.business.BusinessSummaryLookup;
import io.resrv.platform.exchange.business.BusinessSummaryView;
import io.resrv.platform.exchange.membership.BusinessAccessCheck;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PlatformExchangeTestConfiguration {

    @Bean
    @Primary
    PlatformExchangeFixture platformExchangeFixture() {
        return new PlatformExchangeFixture();
    }

    public static final class PlatformExchangeFixture
            implements ActiveBusinessLookup, BusinessSummaryLookup, BusinessAccessCheck {

        private final Map<BusinessId, BusinessState> businessesById = new HashMap<>();
        private final Map<String, BusinessId> businessIdsBySlug = new HashMap<>();
        private final Set<AccessGrant> accessGrants = new HashSet<>();

        public void reset() {
            businessesById.clear();
            businessIdsBySlug.clear();
            accessGrants.clear();
        }

        public void putBusiness(
                final BusinessId id,
                final String name,
                final String slug,
                final Timezone timezone,
                final boolean active) {
            final var state = new BusinessState(id, name, slug, timezone, active);
            businessesById.put(id, state);
            businessIdsBySlug.put(slug, id);
        }

        public void setBusinessActive(final BusinessId id, final boolean active) {
            final var existing = businessesById.get(id);
            if (existing == null) {
                return;
            }
            businessesById.put(
                    id,
                    new BusinessState(
                            existing.id(),
                            existing.name(),
                            existing.slug(),
                            existing.timezone(),
                            active));
        }

        public void grantAccess(final AccountId accountId, final BusinessId businessId) {
            accessGrants.add(new AccessGrant(accountId, businessId));
        }

        public void revokeAccess(final AccountId accountId, final BusinessId businessId) {
            accessGrants.remove(new AccessGrant(accountId, businessId));
        }

        @Override
        public Optional<ActiveBusinessView> findActiveById(final BusinessId businessId) {
            return Optional.ofNullable(businessesById.get(businessId))
                    .filter(BusinessState::active)
                    .map(BusinessState::toActiveView);
        }

        @Override
        public Optional<ActiveBusinessView> findActiveBySlug(final String slug) {
            return Optional.ofNullable(businessIdsBySlug.get(slug)).flatMap(this::findActiveById);
        }

        @Override
        public Optional<BusinessSummaryView> findCurrentSummaryById(final BusinessId businessId) {
            return Optional.ofNullable(businessesById.get(businessId))
                    .map(BusinessState::toSummaryView);
        }

        @Override
        public Optional<BusinessSummaryView> findCurrentSummaryBySlug(final String slug) {
            return Optional.ofNullable(businessIdsBySlug.get(slug))
                    .flatMap(this::findCurrentSummaryById);
        }

        @Override
        public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
            return findActiveById(businessId).isPresent()
                    && accessGrants.contains(new AccessGrant(accountId, businessId));
        }
    }

    private record BusinessState(
            BusinessId id, String name, String slug, Timezone timezone, boolean active) {

        ActiveBusinessView toActiveView() {
            return new ActiveBusinessView(id, name, slug, timezone);
        }

        BusinessSummaryView toSummaryView() {
            return new BusinessSummaryView(id, name, slug, timezone);
        }
    }

    private record AccessGrant(AccountId accountId, BusinessId businessId) {}
}

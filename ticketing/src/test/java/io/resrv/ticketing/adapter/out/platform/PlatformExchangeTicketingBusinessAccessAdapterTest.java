package io.resrv.ticketing.adapter.out.platform;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.platform.exchange.business.ActiveBusinessLookup;
import io.resrv.platform.exchange.business.ActiveBusinessView;
import io.resrv.platform.exchange.membership.BusinessAccessCheck;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PlatformExchangeTicketingBusinessAccessAdapterTest {

    private static final Timezone SEOUL = Timezone.of("Asia/Seoul");

    private final FakeActiveBusinessLookup activeBusinessLookup = new FakeActiveBusinessLookup();
    private final FakeBusinessAccessCheck businessAccessCheck = new FakeBusinessAccessCheck();
    private final PlatformExchangeTicketingBusinessAccessAdapter adapter =
            new PlatformExchangeTicketingBusinessAccessAdapter(
                    activeBusinessLookup, businessAccessCheck);

    @Test
    void mapsActiveBusinessLookupToTicketingBusinessView() {
        final var businessId = BusinessId.create();
        activeBusinessLookup.addBusiness(businessId);

        final var result = adapter.findActiveBusiness(businessId);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().id()).isEqualTo(businessId);
        assertThat(result.orElseThrow().name()).isEqualTo("Business");
        assertThat(result.orElseThrow().timezone()).isEqualTo(SEOUL);
    }

    @Test
    void delegatesBusinessAccessCheckToPlatformExchange() {
        final var accountId = AccountId.create();
        final var businessId = BusinessId.create();
        businessAccessCheck.allow(accountId, businessId);

        assertThat(adapter.hasBusinessAccess(accountId, businessId)).isTrue();
        assertThat(adapter.hasBusinessAccess(AccountId.create(), businessId)).isFalse();
    }

    private static final class FakeActiveBusinessLookup implements ActiveBusinessLookup {

        private final Map<BusinessId, ActiveBusinessView> businesses = new HashMap<>();

        void addBusiness(final BusinessId businessId) {
            businesses.put(
                    businessId, new ActiveBusinessView(businessId, "Business", "business", SEOUL));
        }

        @Override
        public Optional<ActiveBusinessView> findActiveById(final BusinessId businessId) {
            return Optional.ofNullable(businesses.get(businessId));
        }

        @Override
        public Optional<ActiveBusinessView> findActiveBySlug(final String slug) {
            return businesses.values().stream()
                    .filter(business -> business.slug().equals(slug))
                    .findFirst();
        }
    }

    private static final class FakeBusinessAccessCheck implements BusinessAccessCheck {

        private final Map<AccountId, BusinessId> allowedMemberships = new HashMap<>();

        void allow(final AccountId accountId, final BusinessId businessId) {
            allowedMemberships.put(accountId, businessId);
        }

        @Override
        public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
            return businessId.equals(allowedMemberships.get(accountId));
        }
    }
}

package io.resrv.timeslot.adapter.out.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.contract.business.ActiveBusinessLookup;
import io.resrv.platform.contract.business.ActiveBusinessView;
import io.resrv.platform.contract.business.BusinessSummaryLookup;
import io.resrv.platform.contract.business.BusinessSummaryView;
import io.resrv.platform.contract.membership.BusinessAccessCheck;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PlatformBusinessLookupAdapterTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Timezone TIMEZONE = Timezone.of("Asia/Seoul");

    private final ActiveBusinessLookup activeBusinessLookup = mock(ActiveBusinessLookup.class);
    private final BusinessSummaryLookup businessSummaryLookup = mock(BusinessSummaryLookup.class);
    private final BusinessAccessCheck businessAccessCheck = mock(BusinessAccessCheck.class);
    private final PlatformBusinessLookupAdapter adapter =
            new PlatformBusinessLookupAdapter(
                    activeBusinessLookup, businessSummaryLookup, businessAccessCheck);

    @Test
    void mapsPlatformBusinessContractToTimeslotBusinessView() {
        when(activeBusinessLookup.findActiveById(BUSINESS_ID))
                .thenReturn(
                        Optional.of(
                                new ActiveBusinessView(
                                        BUSINESS_ID, "Salon A", "salon-a", TIMEZONE)));

        final var business = adapter.findActiveById(BUSINESS_ID).orElseThrow();

        assertEquals(BUSINESS_ID, business.id());
        assertEquals("Salon A", business.name());
        assertEquals("salon-a", business.slug());
        assertEquals(TIMEZONE, business.timezone());
    }

    @Test
    void mapsMissingPlatformBusinessToEmpty() {
        when(activeBusinessLookup.findActiveById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertTrue(adapter.findActiveById(BUSINESS_ID).isEmpty());
    }

    @Test
    void mapsCurrentPlatformBusinessSummaryToTimeslotBusinessView() {
        when(businessSummaryLookup.findCurrentSummaryById(BUSINESS_ID))
                .thenReturn(
                        Optional.of(
                                new BusinessSummaryView(
                                        BUSINESS_ID, "Salon A", "salon-a", TIMEZONE)));

        final var business = adapter.findCurrentSummaryById(BUSINESS_ID).orElseThrow();

        assertEquals(BUSINESS_ID, business.id());
        assertEquals("Salon A", business.name());
        assertEquals("salon-a", business.slug());
        assertEquals(TIMEZONE, business.timezone());
    }

    @Test
    void delegatesBusinessAccessCheckToPlatformContract() {
        when(businessAccessCheck.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID)).thenReturn(true);

        assertTrue(adapter.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));

        when(businessAccessCheck.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID)).thenReturn(false);

        assertFalse(adapter.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }
}

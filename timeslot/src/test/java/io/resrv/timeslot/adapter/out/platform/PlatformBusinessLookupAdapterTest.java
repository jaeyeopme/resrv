package io.resrv.timeslot.adapter.out.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.business.in.LookupActiveBusinessResult;
import io.resrv.platform.application.business.in.LookupActiveBusinessUseCase;
import io.resrv.platform.application.membership.in.CheckBusinessAccessUseCase;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PlatformBusinessLookupAdapterTest {

    private static final AccountId ACCOUNT_ID = AccountId.create();
    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Timezone TIMEZONE = Timezone.of("Asia/Seoul");

    private final LookupActiveBusinessUseCase lookupActiveBusinessUseCase =
            mock(LookupActiveBusinessUseCase.class);
    private final CheckBusinessAccessUseCase checkBusinessAccessUseCase =
            mock(CheckBusinessAccessUseCase.class);
    private final PlatformBusinessLookupAdapter adapter =
            new PlatformBusinessLookupAdapter(
                    lookupActiveBusinessUseCase, checkBusinessAccessUseCase);

    @Test
    void mapsPlatformBusinessContractToTimeslotBusinessView() {
        when(lookupActiveBusinessUseCase.findActiveById(BUSINESS_ID))
                .thenReturn(
                        Optional.of(
                                new LookupActiveBusinessResult(
                                        BUSINESS_ID, "Salon A", "salon-a", TIMEZONE)));

        final var business = adapter.findActiveById(BUSINESS_ID).orElseThrow();

        assertEquals(BUSINESS_ID, business.id());
        assertEquals("Salon A", business.name());
        assertEquals("salon-a", business.slug());
        assertEquals(TIMEZONE, business.timezone());
    }

    @Test
    void mapsMissingPlatformBusinessToEmpty() {
        when(lookupActiveBusinessUseCase.findActiveById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertTrue(adapter.findActiveById(BUSINESS_ID).isEmpty());
    }

    @Test
    void delegatesBusinessAccessCheckToPlatformContract() {
        when(checkBusinessAccessUseCase.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID))
                .thenReturn(true);

        assertTrue(adapter.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));

        when(checkBusinessAccessUseCase.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID))
                .thenReturn(false);

        assertFalse(adapter.hasBusinessAccess(ACCOUNT_ID, BUSINESS_ID));
    }
}

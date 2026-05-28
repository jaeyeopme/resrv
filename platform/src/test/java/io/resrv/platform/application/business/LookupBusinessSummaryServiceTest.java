package io.resrv.platform.application.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessStatus;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class LookupBusinessSummaryServiceTest {

    private static final BusinessId BUSINESS_ID = BusinessId.create();
    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");

    private BusinessQueryPort businessQueryPort;
    private LookupBusinessSummaryService service;

    @BeforeEach
    void setUp() {
        businessQueryPort = mock(BusinessQueryPort.class);
        service = new LookupBusinessSummaryService(businessQueryPort);
    }

    @Test
    void findsActiveBusinessSummary() {
        when(businessQueryPort.findById(BUSINESS_ID))
                .thenReturn(Optional.of(business(BusinessStatus.ACTIVE)));

        final var business = service.findCurrentSummaryById(BUSINESS_ID).orElseThrow();

        assertEquals(BUSINESS_ID, business.id());
        assertEquals("Salon A", business.name());
        assertEquals("salon-a", business.slug());
        assertEquals(Timezone.of("Asia/Seoul"), business.timezone());
    }

    @Test
    void findsInactiveBusinessSummary() {
        when(businessQueryPort.findById(BUSINESS_ID))
                .thenReturn(Optional.of(business(BusinessStatus.INACTIVE)));

        final var business = service.findCurrentSummaryById(BUSINESS_ID).orElseThrow();

        assertEquals(BUSINESS_ID, business.id());
    }

    @Test
    void missingBusinessSummaryIsEmpty() {
        when(businessQueryPort.findById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertTrue(service.findCurrentSummaryById(BUSINESS_ID).isEmpty());
    }

    private static Business business(final BusinessStatus status) {
        return Business.reconstitute(
                BUSINESS_ID,
                new BusinessName("Salon A"),
                new BusinessSlug("salon-a"),
                Timezone.of("Asia/Seoul"),
                status,
                NOW);
    }
}

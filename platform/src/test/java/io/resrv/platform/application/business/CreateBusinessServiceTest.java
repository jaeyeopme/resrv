package io.resrv.platform.application.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.resrv.platform.application.business.in.CreateBusinessCommand;
import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.application.membership.out.BusinessMembershipCommandPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import io.resrv.platform.domain.membership.BusinessMembership;
import io.resrv.platform.domain.membership.BusinessRole;
import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.Timezone;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateBusinessServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private BusinessCommandPort businessCommandPort;
    private BusinessQueryPort businessQueryPort;
    private BusinessMembershipCommandPort membershipCommandPort;
    private CreateBusinessService service;

    @BeforeEach
    void setUp() {
        businessCommandPort = mock(BusinessCommandPort.class);
        businessQueryPort = mock(BusinessQueryPort.class);
        membershipCommandPort = mock(BusinessMembershipCommandPort.class);
        service =
                new CreateBusinessService(
                        businessCommandPort,
                        businessQueryPort,
                        membershipCommandPort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsBusinessAndOwnerMembership() {
        final var ownerAccountId = AccountId.create();
        final var slug = new BusinessSlug("owner-studio");
        when(businessQueryPort.findBySlug(slug)).thenReturn(Optional.empty());

        final var result =
                service.create(
                        new CreateBusinessCommand(
                                ownerAccountId, " Owner Studio ", "owner-studio", "Asia/Seoul"));

        assertEquals("Owner Studio", result.name());
        assertEquals("owner-studio", result.slug());
        assertEquals("Asia/Seoul", result.timezone());

        final var businessCaptor = ArgumentCaptor.forClass(Business.class);
        verify(businessCommandPort).save(businessCaptor.capture());
        final var savedBusiness = businessCaptor.getValue();
        assertEquals(savedBusiness.id().value(), result.id());
        assertEquals(NOW, savedBusiness.createdAt());

        final var membershipCaptor = ArgumentCaptor.forClass(BusinessMembership.class);
        verify(membershipCommandPort).save(membershipCaptor.capture());
        final var membership = membershipCaptor.getValue();
        assertEquals(ownerAccountId, membership.accountId());
        assertEquals(savedBusiness.id(), membership.businessId());
        assertEquals(BusinessRole.OWNER, membership.role());
        assertEquals(NOW, membership.createdAt());
    }

    @Test
    void duplicateSlugThrowsAndDoesNotSaveBusinessOrMembership() {
        final var slug = new BusinessSlug("owner-studio");
        final var existing =
                Business.create(
                        new BusinessName("Owner Studio"), slug, Timezone.of("Asia/Seoul"), NOW);
        when(businessQueryPort.findBySlug(slug)).thenReturn(Optional.of(existing));

        final var exception =
                assertThrows(
                        BusinessSlugAlreadyExistsException.class,
                        () ->
                                service.create(
                                        new CreateBusinessCommand(
                                                AccountId.create(),
                                                "Owner Studio",
                                                "owner-studio",
                                                "Asia/Seoul")));

        assertEquals("Business slug already exists: owner-studio", exception.getMessage());
        verify(businessCommandPort, never()).save(any());
        verify(membershipCommandPort, never()).save(any());
    }

    @Test
    void nullOwnerAccountIdRejectedByCommand() {
        final var exception =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                new CreateBusinessCommand(
                                        null, "Owner Studio", "owner-studio", "Asia/Seoul"));

        assertEquals("Owner account id must not be null", exception.getMessage());
    }
}

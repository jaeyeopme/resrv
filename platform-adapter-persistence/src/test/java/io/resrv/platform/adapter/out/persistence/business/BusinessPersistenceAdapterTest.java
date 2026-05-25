package io.resrv.platform.adapter.out.persistence.business;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.shared.kernel.Timezone;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BusinessPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void save_whenNonSlugConstraintFails_rethrowsPersistenceException() {
        final var repository = mock(BusinessJpaRepository.class);
        final var entityManager = mock(EntityManager.class);
        final var adapter = new BusinessPersistenceAdapter(repository, entityManager);
        final var business = createBusiness();
        when(repository.save(any(BusinessJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new PersistenceException("ck_platform_business_timezone_not_blank"))
                .when(entityManager)
                .flush();

        assertThrows(PersistenceException.class, () -> adapter.save(business));
    }

    private static Business createBusiness() {
        return Business.create(
                new BusinessName("Studio One"),
                new BusinessSlug("studio-one"),
                Timezone.of("Asia/Seoul"),
                NOW);
    }
}

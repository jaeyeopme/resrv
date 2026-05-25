package io.resrv.platform.adapter.out.persistence.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.platform.application.business.out.BusinessCommandPort;
import io.resrv.platform.application.business.out.BusinessQueryPort;
import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessSlugAlreadyExistsException;
import io.resrv.shared.kernel.Timezone;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BusinessPersistenceAdapter.class)
class PlatformBusinessPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private BusinessCommandPort commandPort;

    @Autowired private BusinessQueryPort queryPort;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindByIdAndSlug() {
        final var business = createBusiness("Studio One", "studio-one");

        commandPort.save(business);

        final var foundById = queryPort.findById(business.id()).orElseThrow();
        assertEquals(business.id(), foundById.id());
        assertEquals("Studio One", foundById.name().value());
        assertEquals("studio-one", foundById.slug().value());
        assertEquals("Asia/Seoul", foundById.timezone().value().getId());
        assertTrue(foundById.active());

        final var foundBySlug = queryPort.findBySlug(new BusinessSlug("studio-one"));
        assertTrue(foundBySlug.isPresent());
        assertEquals(business.id(), foundBySlug.orElseThrow().id());
    }

    @Test
    void duplicateSlug_throwsBusinessSlugAlreadyExistsException() {
        commandPort.save(createBusiness("Studio One", "studio-one"));

        assertThrows(
                BusinessSlugAlreadyExistsException.class,
                () -> commandPort.save(createBusiness("Studio Two", "studio-one")));
    }

    @Test
    void blankSlug_isRejectedByDatabaseConstraint() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertBusinessDirectly("Blank Slug", " ", "Asia/Seoul"));
    }

    @Test
    void blankTimezone_isRejectedByDatabaseConstraint() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertBusinessDirectly("Blank Timezone", "blank-timezone", " "));
    }

    @Test
    void missingBusiness_returnsEmpty() {
        assertFalse(queryPort.findBySlug(new BusinessSlug("missing-business")).isPresent());
    }

    private static Business createBusiness(final String name, final String slug) {
        return Business.create(
                new BusinessName(name), new BusinessSlug(slug), Timezone.of("Asia/Seoul"), NOW);
    }

    private void insertBusinessDirectly(
            final String name, final String slug, final String timezone) {
        jdbcTemplate.update(
                """
                INSERT INTO platform.business
                    (id, name, slug, timezone, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                name,
                slug,
                timezone,
                "ACTIVE",
                Timestamp.from(NOW));
    }
}

package io.resrv.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class ResourceManagementIntegrationTest extends AbstractIntegrationTest {

    private static final Instant FIXTURE_TIME = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void resourceLifecycle_isTenantScopedAndSoftDeletes() throws Exception {
        final var tenantId = insertTenant("resource-lifecycle");
        final var authorization = bearerToken(tenantId);

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Room A", "room-a", "Quiet room")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Room A"))
                .andExpect(jsonPath("$.slug").value("room-a"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        final var resourceId = findResourceId(tenantId, "room-a");

        mockMvc.perform(get("/api/resources").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(resourceId.toString()))
                .andExpect(jsonPath("$[0].slug").value("room-a"));

        mockMvc.perform(
                        put("/api/resources/{resourceId}", resourceId)
                                .header(HttpHeaders.AUTHORIZATION, authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Room B", "room-b", "Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.name").value("Room B"))
                .andExpect(jsonPath("$.slug").value("room-b"))
                .andExpect(jsonPath("$.description").value("Updated"));

        mockMvc.perform(
                        delete("/api/resources/{resourceId}", resourceId)
                                .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        final var status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM resource WHERE id = ?", String.class, resourceId);
        assertThat(status).isEqualTo("INACTIVE");

        mockMvc.perform(get("/api/resources").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(
                        get("/api/resources/{resourceId}", resourceId)
                                .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void sameSlugAllowedAcrossDifferentTenants() throws Exception {
        final var firstTenantId = insertTenant("resource-slug-first");
        final var secondTenantId = insertTenant("resource-slug-second");

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(firstTenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("First Room", "shared-room", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(secondTenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Second Room", "shared-room", null)))
                .andExpect(status().isCreated());

        final var count =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM resource WHERE slug = 'shared-room'", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void duplicateSlugInSameTenant_returns409() throws Exception {
        final var tenantId = insertTenant("resource-duplicate");
        final var authorization = bearerToken(tenantId);

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Room A", "room-a", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Duplicate Room", "room-a", null)))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Resource slug 'room-a' is already in use for this tenant"));
    }

    @Test
    void resourceIdFromOtherTenant_returns404() throws Exception {
        final var firstTenantId = insertTenant("resource-scope-first");
        final var secondTenantId = insertTenant("resource-scope-second");

        mockMvc.perform(
                        post("/api/resources")
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(firstTenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resourceJson("Room A", "room-a", null)))
                .andExpect(status().isCreated());
        final var resourceId = findResourceId(firstTenantId, "room-a");

        mockMvc.perform(
                        get("/api/resources/{resourceId}", resourceId)
                                .header(HttpHeaders.AUTHORIZATION, bearerToken(secondTenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiResourcesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/resources")).andExpect(status().isUnauthorized());
    }

    private UUID insertTenant(final String slugPrefix) {
        final var id = UUID.randomUUID();
        final var slug = slugPrefix + "-" + Math.abs(System.nanoTime());
        jdbcTemplate.update(
                """
                INSERT INTO tenant (
                    id, name, slug, timezone, slot_duration, hold_ttl,
                    cancellation_window, status, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "Tenant " + slugPrefix,
                slug,
                "UTC",
                30,
                15,
                0,
                "ACTIVE",
                Timestamp.from(FIXTURE_TIME));
        return id;
    }

    private UUID findResourceId(final UUID tenantId, final String slug) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resource WHERE tenant_id = ? AND slug = ?",
                UUID.class,
                tenantId,
                slug);
    }

    private String bearerToken(final UUID tenantId) throws Exception {
        final var now = Instant.now();
        return "Bearer "
                + mintJwt(UUID.randomUUID(), tenantId, "OWNER", now, now.plusSeconds(1800));
    }

    private static String resourceJson(
            final String name, final String slug, final String description) {
        if (description == null) {
            return """
                    {
                        "name": "%s",
                        "slug": "%s"
                    }
                    """
                    .formatted(name, slug);
        }
        return """
                {
                    "name": "%s",
                    "slug": "%s",
                    "description": "%s"
                }
                """
                .formatted(name, slug, description);
    }
}

package io.resrv.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void happyPath_fullRegistration() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "마이살롱",
                                                "integration-test-salon",
                                                "Asia/Seoul",
                                                60,
                                                "owner@example.com",
                                                "securepass123")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.slug").value("integration-test-salon"));

        final var tenantCount =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM tenant WHERE slug = 'integration-test-salon'",
                        Integer.class);
        assertThat(tenantCount).isEqualTo(1);

        final var adminCount =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM admin WHERE email = 'owner@example.com'",
                        Integer.class);
        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    void duplicateSlug_returns409() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "First",
                                                "duplicate-test",
                                                "UTC",
                                                30,
                                                "first@example.com",
                                                "password123")))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "Second",
                                                "duplicate-test",
                                                "UTC",
                                                30,
                                                "second@example.com",
                                                "password123")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidFields_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "name": "",
                                    "slug": "A",
                                    "timezone": "UTC",
                                    "slotDuration": 45,
                                    "admin": {
                                        "email": "invalid",
                                        "password": "short"
                                    }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordIsArgon2Hashed() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "Hash Test",
                                                "argon2-test",
                                                "UTC",
                                                30,
                                                "argon2@example.com",
                                                "plaintext123")))
                .andExpect(status().isCreated());

        final var hashedPassword =
                jdbcTemplate.queryForObject(
                        "SELECT hashed_password FROM admin WHERE email = 'argon2@example.com'",
                        String.class);

        assertThat(hashedPassword).isNotEqualTo("plaintext123").startsWith("$argon2");
    }

    @Test
    void omittedOptionalFields_defaultsPersisted() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "Defaults Test",
                                                "defaults-test",
                                                "UTC",
                                                30,
                                                "defaults@example.com",
                                                "password123")))
                .andExpect(status().isCreated());

        final var holdTtl =
                jdbcTemplate.queryForObject(
                        "SELECT hold_ttl FROM tenant WHERE slug = 'defaults-test'", Integer.class);
        assertThat(holdTtl).isEqualTo(15);

        final var cancellationWindow =
                jdbcTemplate.queryForObject(
                        "SELECT cancellation_window FROM tenant WHERE slug = 'defaults-test'",
                        Integer.class);
        assertThat(cancellationWindow).isZero();
    }

    @Test
    void adminHasOwnerRole() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        registrationJson(
                                                "Role Test",
                                                "role-test",
                                                "UTC",
                                                30,
                                                "role@example.com",
                                                "password123")))
                .andExpect(status().isCreated());

        final var role =
                jdbcTemplate.queryForObject(
                        "SELECT role FROM admin WHERE email = 'role@example.com'", String.class);
        assertThat(role).isEqualTo("OWNER");
    }

    private static String registrationJson(
            final String name,
            final String slug,
            final String timezone,
            final int slotDuration,
            final String email,
            final String password) {
        return """
                {
                    "name": "%s",
                    "slug": "%s",
                    "timezone": "%s",
                    "slotDuration": %d,
                    "admin": {
                        "email": "%s",
                        "password": "%s"
                    }
                }
                """
                .formatted(name, slug, timezone, slotDuration, email, password);
    }
}

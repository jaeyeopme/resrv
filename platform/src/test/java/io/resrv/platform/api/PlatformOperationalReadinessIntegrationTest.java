package io.resrv.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///resrv",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "resrv.jwt.secret-key=01234567890123456789012345678901",
            "resrv.jwt.issuer=resrv-test",
            "resrv.jwt.audience=resrv-api",
            "resrv.jwt.expiration=3600",
            "resrv.security.password-reset.public-base-url=https://app.example.com",
            "resrv.security.password-reset.token-ttl=PT30M"
        })
@AutoConfigureMockMvc
@Import(FakePasswordResetEmailAdapter.class)
final class PlatformOperationalReadinessIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void healthProbesArePublicAndDependencyAware() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void healthProbesDoNotExposeSecretsOrPrivateDomainData() throws Exception {
        final var content =
                mockMvc.perform(get("/actuator/health/readiness"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(content).doesNotContain(JWT_SECRET);
        assertThat(content).doesNotContain("secret");
        assertThat(content).doesNotContain("password");
        assertThat(content).doesNotContain("accountId");
        assertThat(content).doesNotContain("businessId");
        assertThat(content).doesNotContain("reservation");
    }

    @Test
    void platformRuntimeAppliesPlatformAndTimeslotMigrations() {
        final List<String> scripts =
                jdbcTemplate.queryForList(
                        "SELECT script FROM flyway_schema_history WHERE success = true",
                        String.class);

        assertThat(scripts).contains("V9__create_platform_schema.sql");
        assertThat(scripts).contains("V10__create_timeslot_schema.sql");
        assertThat(regclass("platform.account")).isEqualTo("platform.account");
        assertThat(regclass("timeslot.reservation")).isEqualTo("timeslot.reservation");
    }

    @Test
    void generatedApiDocumentationRemainsReachableForSmokeChecks() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/accounts'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/businesses/{businessId}/reservations'].post")
                                .exists());
    }

    @Test
    void operationsDocsPreserveSupportedRuntimeAndExcludeUnsupportedServices() throws Exception {
        final var docs =
                Files.readString(projectRoot().resolve("README.md"))
                        + "\n"
                        + Files.readString(projectRoot().resolve("docs/operations.md"))
                        + "\n"
                        + Files.readString(projectRoot().resolve("docs/trd.md"));

        assertThat(docs).contains("platform` is the canonical backend runtime");
        assertThat(docs).contains("standalone timeslot runtime");
        assertThat(docs).contains("execution is not a supported operation");
        assertThat(docs).doesNotContain("payment service is supported");
        assertThat(docs).doesNotContain("notification service is supported");
        assertThat(docs).doesNotContain("outbox worker is supported");
        assertThat(docs).doesNotContain("separate timeslot service is supported");
    }

    private String regclass(final String qualifiedName) {
        return jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)::text", String.class, qualifiedName);
    }

    private static Path projectRoot() {
        final var userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(userDir.resolve("settings.gradle.kts"))) {
            return userDir;
        }
        return userDir.getParent();
    }
}

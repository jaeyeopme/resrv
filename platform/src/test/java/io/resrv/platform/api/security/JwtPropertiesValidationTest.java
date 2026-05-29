package io.resrv.platform.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

final class JwtPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfig.class)
                    .withPropertyValues(
                            "resrv.jwt.secret-key=01234567890123456789012345678901",
                            "resrv.jwt.issuer=resrv-test",
                            "resrv.jwt.audience=resrv-api",
                            "resrv.jwt.expiration=3600");

    private final ApplicationContextRunner passwordResetContextRunner =
            new ApplicationContextRunner().withUserConfiguration(PasswordResetTestConfig.class);

    @Test
    void rejectsShortSecretKey() {
        contextRunner
                .withPropertyValues("resrv.jwt.secret-key=short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankIssuer() {
        contextRunner
                .withPropertyValues("resrv.jwt.issuer= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankAudience() {
        contextRunner
                .withPropertyValues("resrv.jwt.audience= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveExpiration() {
        contextRunner
                .withPropertyValues("resrv.jwt.expiration=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void usesPasswordResetOperationalDefaultsWhenUnset() {
        passwordResetContextRunner.run(
                context -> {
                    final var properties =
                            context.getBean(PlatformSecurityConfig.PasswordResetProperties.class);

                    assertThat(properties.publicBaseUrl()).isEqualTo("http://localhost:8080");
                    assertThat(properties.tokenTtl()).isEqualTo(Duration.ofMinutes(30));
                });
    }

    @Test
    void acceptsExplicitPasswordResetOperationalSettings() {
        passwordResetContextRunner
                .withPropertyValues(
                        "resrv.security.password-reset.public-base-url=https://app.example.com",
                        "resrv.security.password-reset.token-ttl=PT45M")
                .run(
                        context -> {
                            final var properties =
                                    context.getBean(
                                            PlatformSecurityConfig.PasswordResetProperties.class);

                            assertThat(properties.publicBaseUrl())
                                    .isEqualTo("https://app.example.com");
                            assertThat(properties.tokenTtl()).isEqualTo(Duration.ofMinutes(45));
                        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PlatformSecurityConfig.JwtProperties.class)
    private static class TestConfig {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PlatformSecurityConfig.PasswordResetProperties.class)
    private static class PasswordResetTestConfig {}
}

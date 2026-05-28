package io.resrv.timeslot.api.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TimeslotSecurityConfig.JwtProperties.class)
class TimeslotSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain timeslotDocumentationSecurityFilterChain(final HttpSecurity http) {
        return http.securityMatcher(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain timeslotApiSecurityFilterChain(
            final HttpSecurity http, final JwtDecoder jwtDecoder) {
        return http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                HttpMethod.GET,
                                                "/api/public/businesses/*",
                                                "/api/public/businesses/*/resources",
                                                "/api/public/businesses/*/resources/*/slots",
                                                "/api/businesses/*/resources",
                                                "/api/businesses/*/resources/*/slots")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(final JwtProperties properties) {
        final var decoder = buildDecoder(properties);
        decoder.setJwtValidator(jwtValidator(properties));
        return decoder;
    }

    private static NimbusJwtDecoder buildDecoder(final JwtProperties properties) {
        final var key =
                new SecretKeySpec(
                        properties.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static OAuth2TokenValidator<Jwt> jwtValidator(final JwtProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                audienceValidator(properties),
                requiredClaimsValidator());
    }

    private static JwtClaimValidator<List<String>> audienceValidator(
            final JwtProperties properties) {
        return new JwtClaimValidator<>(
                "aud", audience -> audience != null && audience.contains(properties.audience()));
    }

    private static OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            final var jwtId = jwt.getId();
            if (isBlank(jwtId)) {
                return tokenFailure("jti claim must not be blank");
            }

            final var accountId = jwt.getClaimAsString("accountId");
            final var parsedAccountId = parseUuid(accountId);
            if (parsedAccountId == null) {
                return tokenFailure("accountId claim must be a UUID");
            }

            final var subject = jwt.getSubject();
            final var parsedSubject = parseUuid(subject);
            if (parsedSubject == null) {
                return tokenFailure("sub claim must be a UUID");
            }
            if (!subject.equals(accountId)) {
                return tokenFailure("sub claim must match accountId claim");
            }

            return OAuth2TokenValidatorResult.success();
        };
    }

    private static UUID parseUuid(final String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            final var uuid = UUID.fromString(value);
            return uuid.toString().equalsIgnoreCase(value) ? uuid : null;
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static OAuth2TokenValidatorResult tokenFailure(final String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }

    @ConfigurationProperties(prefix = "resrv.jwt")
    @Validated
    record JwtProperties(
            @NotBlank(message = "JWT secret key must not be blank")
                    @Size(min = 32, message = "JWT secret key must be at least 32 characters")
                    String secretKey,
            @NotBlank(message = "JWT issuer must not be blank") String issuer,
            @NotBlank(message = "JWT audience must not be blank") String audience,
            @Positive(message = "JWT expiration must be positive") long expiration) {

        @AssertTrue(message = "JWT secret key must be at least 32 bytes")
        public boolean isSecretKeyAtLeast32Bytes() {
            return secretKey != null && secretKey.getBytes(StandardCharsets.UTF_8).length >= 32;
        }
    }
}

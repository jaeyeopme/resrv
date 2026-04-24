package io.resrv.bootstrap.security;

import static io.resrv.application.auth.TokenClaimNames.JTI;
import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;

import io.resrv.application.auth.out.TokenRevocationPort;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain apiDocumentationFilterChain(final HttpSecurity http) {
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
    SecurityFilterChain logoutFilterChain(
            final HttpSecurity http, final JwtProperties jwtProperties, final ObjectMapper mapper) {
        final var decoder = buildDecoder(jwtProperties);
        decoder.setJwtValidator(lenientValidator(jwtProperties));

        return http.securityMatcher(
                        request ->
                                HttpMethod.POST.name().equals(request.getMethod())
                                        && "/api/auth/logout".equals(request.getRequestURI()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(jwt -> jwt.decoder(decoder))
                                        .authenticationEntryPoint(
                                                new ProblemDetailAuthEntryPoint(mapper)))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(
            final HttpSecurity http,
            final JwtProperties jwtProperties,
            final TokenRevocationPort tokenRevocationPort,
            final ObjectMapper mapper) {
        final var decoder = buildDecoder(jwtProperties);
        decoder.setJwtValidator(strictValidator(jwtProperties));

        return http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.POST, "/api/tenants")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(jwt -> jwt.decoder(decoder))
                                        .authenticationEntryPoint(
                                                new ProblemDetailAuthEntryPoint(mapper)))
                .addFilterAfter(
                        new JtiBlacklistFilter(tokenRevocationPort, mapper),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicFilterChain(final HttpSecurity http) {
        return http.securityMatcher("/public/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    private static NimbusJwtDecoder buildDecoder(final JwtProperties properties) {
        final var key =
                new SecretKeySpec(
                        properties.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static OAuth2TokenValidator<Jwt> strictValidator(final JwtProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                audienceValidator(properties),
                requiredClaimsValidator());
    }

    private static OAuth2TokenValidator<Jwt> lenientValidator(final JwtProperties properties) {
        // No expiry check — allows expired tokens for logout
        return new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(properties.issuer()),
                audienceValidator(properties),
                requiredClaimsValidator());
    }

    private static JwtClaimValidator<List<String>> audienceValidator(
            final JwtProperties properties) {
        return new JwtClaimValidator<>("aud", aud -> aud.contains(properties.audience()));
    }

    private static OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return new DelegatingOAuth2TokenValidator<>(
                nonBlankClaim(JTI),
                nonBlankClaim(USER_ID),
                nonBlankClaim(TENANT_ID),
                nonBlankClaim(ROLE));
    }

    private static JwtClaimValidator<String> nonBlankClaim(final String name) {
        return new JwtClaimValidator<>(name, v -> v != null && !v.isBlank());
    }
}

package io.resrv.platform.api.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PlatformSecurityConfig.JwtProperties.class)
class PlatformSecurityConfig {

    @Bean
    SecurityFilterChain platformSecurityFilterChain(
            final HttpSecurity http, final JwtDecoder jwtDecoder) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                HttpMethod.POST, "/api/accounts", "/api/auth/login")
                                        .permitAll()
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
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
        return new DelegatingOAuth2TokenValidator<>(
                nonBlankClaim("jti"), nonBlankClaim("accountId"), nonBlankClaim("sub"));
    }

    private static JwtClaimValidator<String> nonBlankClaim(final String name) {
        return new JwtClaimValidator<>(name, value -> value != null && !value.isBlank());
    }

    @ConfigurationProperties(prefix = "resrv.jwt")
    record JwtProperties(String secretKey, String issuer, String audience, long expiration) {}
}

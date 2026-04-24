package io.resrv.adapter.in.web.auth;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.in.LogoutCommand;
import io.resrv.application.auth.in.LogoutUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AuthWebAdapter.class)
class AuthWebAdapterTest {

    private static final String USER_ID_VALUE = "00000000-0000-0000-0000-000000000001";
    private static final String TENANT_ID_VALUE = "00000000-0000-0000-0000-000000000002";
    private static final String JTI_VALUE = UUID.randomUUID().toString();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LogoutUseCase logoutUseCase;

    @Test
    void logout_success_returns204() throws Exception {
        final var expiry = Instant.parse("2025-12-31T00:00:00Z");

        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(
                                        jwtPrincipal(
                                                b ->
                                                        b.claim(JwtClaimNames.JTI, JTI_VALUE)
                                                                .expiresAt(expiry))))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout(new LogoutCommand(JTI_VALUE, expiry));
    }

    @Test
    void logout_withoutExpiry_usesEpoch() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(jwtPrincipal(b -> b.claim(JwtClaimNames.JTI, JTI_VALUE))))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout(new LogoutCommand(JTI_VALUE, Instant.EPOCH));
    }

    @Test
    void me_success_returns200WithClaims() throws Exception {
        mockMvc.perform(
                        get("/api/auth/me")
                                .with(
                                        jwtPrincipal(
                                                b ->
                                                        b.claim(USER_ID, USER_ID_VALUE)
                                                                .claim(TENANT_ID, TENANT_ID_VALUE)
                                                                .claim(ROLE, "OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID_VALUE))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID_VALUE))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    /**
     * Sets a {@link JwtAuthenticationToken} as the user principal on the mock request, allowing
     * Spring MVC to inject it as a controller method parameter without requiring a fully-configured
     * Spring Security filter chain.
     */
    private static RequestPostProcessor jwtPrincipal(Consumer<Jwt.Builder> customize) {
        return request -> {
            final var builder =
                    Jwt.withTokenValue("test-token").header("alg", "HS256").subject("test");
            customize.accept(builder);
            request.setUserPrincipal(new JwtAuthenticationToken(builder.build(), List.of()));
            return request;
        };
    }
}

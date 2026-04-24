package io.resrv.adapter.in.web.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.auth.LoginResult;
import io.resrv.application.auth.in.LoginCommand;
import io.resrv.application.auth.in.LoginUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginWebAdapter.class)
class LoginWebAdapterTest {

    private static final String TENANT_SLUG = "test-salon";
    private static final String EMAIL = "admin@test.com";
    private static final String PASSWORD = "pass123";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;

    @Test
    void login_success_returns200WithToken() throws Exception {
        when(loginUseCase.login(any(LoginCommand.class)))
                .thenReturn(new LoginResult("test-token", 1800L));

        mockMvc.perform(
                        post("/public/{slug}/auth/login", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "%s", "password": "%s"}
                                        """
                                                .formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(loginUseCase).login(new LoginCommand(TENANT_SLUG, EMAIL, PASSWORD));
    }

    @Test
    void login_authenticationFailed_returns401() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new AuthenticationFailedException());

        mockMvc.perform(
                        post("/public/{slug}/auth/login", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "%s", "password": "wrong"}
                                        """
                                                .formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE));
    }

    @Test
    void login_malformedJson_returns401() throws Exception {
        mockMvc.perform(
                        post("/public/{slug}/auth/login", TENANT_SLUG)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(AuthenticationFailedException.MESSAGE))
                .andExpect(jsonPath("$.instance").value("/public/" + TENANT_SLUG + "/auth/login"));
    }
}

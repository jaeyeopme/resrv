package io.resrv.adapter.in.web.tenant;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.tenant.in.RegisterTenantCommand;
import io.resrv.application.tenant.in.RegisterTenantUseCase;
import io.resrv.domain.tenant.CancellationWindow;
import io.resrv.domain.tenant.HoldTtl;
import io.resrv.domain.tenant.SlotDuration;
import io.resrv.domain.tenant.Slug;
import io.resrv.domain.tenant.SlugAlreadyExistsException;
import io.resrv.domain.tenant.Tenant;
import io.resrv.domain.tenant.TenantName;
import io.resrv.domain.tenant.Timezone;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegisterTenantWebAdapter.class)
class RegisterTenantWebAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterTenantUseCase registerTenantUseCase;

    @Test
    void register_success_returns201WithLocationAndBody() throws Exception {
        final var tenant = createTestTenant("마이살롱", "my-salon", "Asia/Seoul", 60, 15, 0);

        when(registerTenantUseCase.register(any(RegisterTenantCommand.class))).thenReturn(tenant);

        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "name": "마이살롱",
                                    "slug": "my-salon",
                                    "timezone": "Asia/Seoul",
                                    "slotDuration": 60,
                                    "holdTtl": 15,
                                    "cancellationWindow": 0,
                                    "admin": {
                                        "email": "owner@example.com",
                                        "password": "securepass123"
                                    }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("마이살롱"))
                .andExpect(jsonPath("$.slug").value("my-salon"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.slotDuration").value(60))
                .andExpect(jsonPath("$.holdTtl").value(15))
                .andExpect(jsonPath("$.cancellationWindow").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void register_duplicateSlug_returns409() throws Exception {
        when(registerTenantUseCase.register(any()))
                .thenThrow(new SlugAlreadyExistsException(new Slug("my-salon")));

        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "name": "마이살롱",
                                    "slug": "my-salon",
                                    "timezone": "Asia/Seoul",
                                    "slotDuration": 60,
                                    "admin": {
                                        "email": "owner@example.com",
                                        "password": "securepass123"
                                    }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Slug 'my-salon' is already in use"));
    }

    @Test
    void register_invalidInput_returns400WithFieldErrors() throws Exception {
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_omittedOptionalFields_returnsDefaults() throws Exception {
        final var tenant = createTestTenant("Test", "test-slug", "UTC", 30, 15, 0);

        when(registerTenantUseCase.register(any(RegisterTenantCommand.class))).thenReturn(tenant);

        mockMvc.perform(
                        post("/api/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "name": "Test",
                                    "slug": "test-slug",
                                    "timezone": "UTC",
                                    "slotDuration": 30,
                                    "admin": {
                                        "email": "admin@example.com",
                                        "password": "password123"
                                    }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdTtl").value(15))
                .andExpect(jsonPath("$.cancellationWindow").value(0));
    }

    private static Tenant createTestTenant(
            final String name,
            final String slug,
            final String timezone,
            final int slotDuration,
            final int holdTtl,
            final int cancellationWindow) {
        return Tenant.create(
                new TenantName(name),
                new Slug(slug),
                new Timezone(ZoneId.of(timezone)),
                new SlotDuration(slotDuration),
                new HoldTtl(holdTtl),
                new CancellationWindow(cancellationWindow),
                NOW);
    }
}

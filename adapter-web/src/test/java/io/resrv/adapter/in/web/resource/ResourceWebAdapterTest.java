package io.resrv.adapter.in.web.resource;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.resource.in.CreateResourceCommand;
import io.resrv.application.resource.in.CreateResourceUseCase;
import io.resrv.application.resource.in.DeactivateResourceCommand;
import io.resrv.application.resource.in.DeactivateResourceUseCase;
import io.resrv.application.resource.in.GetResourceQuery;
import io.resrv.application.resource.in.GetResourceUseCase;
import io.resrv.application.resource.in.ListResourcesQuery;
import io.resrv.application.resource.in.ListResourcesUseCase;
import io.resrv.application.resource.in.ResourceResult;
import io.resrv.application.resource.in.UpdateResourceCommand;
import io.resrv.application.resource.in.UpdateResourceUseCase;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.resource.ResourceNotFoundException;
import io.resrv.domain.resource.ResourceSlug;
import io.resrv.domain.resource.ResourceSlugAlreadyExistsException;
import io.resrv.domain.resource.ResourceStatus;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ResourceWebAdapter.class)
class ResourceWebAdapterTest {

    private static final UUID TENANT_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESOURCE_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateResourceUseCase createResourceUseCase;

    @MockitoBean private GetResourceUseCase getResourceUseCase;

    @MockitoBean private ListResourcesUseCase listResourcesUseCase;

    @MockitoBean private UpdateResourceUseCase updateResourceUseCase;

    @MockitoBean private DeactivateResourceUseCase deactivateResourceUseCase;

    @Test
    void create_success_returns201WithLocationAndTenantScopedCommand() throws Exception {
        when(createResourceUseCase.create(any(CreateResourceCommand.class)))
                .thenReturn(
                        resourceResult("Room A", "room-a", "Quiet room", ResourceStatus.ACTIVE));

        mockMvc.perform(
                        post("/api/resources")
                                .with(jwtPrincipal())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "name": "Room A",
                                            "slug": "room-a",
                                            "description": "Quiet room"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/resources/" + RESOURCE_ID_VALUE))
                .andExpect(jsonPath("$.id").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.name").value("Room A"))
                .andExpect(jsonPath("$.slug").value("room-a"))
                .andExpect(jsonPath("$.description").value("Quiet room"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value("2025-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2025-01-01T00:00:00Z"));

        final var captor = ArgumentCaptor.forClass(CreateResourceCommand.class);
        verify(createResourceUseCase).create(captor.capture());
        final var command = captor.getValue();
        Assertions.assertThat(command.tenantId().value()).isEqualTo(TENANT_ID_VALUE);
        Assertions.assertThat(command.name()).isEqualTo("Room A");
        Assertions.assertThat(command.slug()).isEqualTo("room-a");
        Assertions.assertThat(command.description()).isEqualTo("Quiet room");
    }

    @Test
    void list_success_returnsActiveResourcesForTenant() throws Exception {
        when(listResourcesUseCase.list(any(ListResourcesQuery.class)))
                .thenReturn(
                        List.of(resourceResult("Room A", "room-a", null, ResourceStatus.ACTIVE)));

        mockMvc.perform(get("/api/resources").with(jwtPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$[0].name").value("Room A"))
                .andExpect(jsonPath("$[0].slug").value("room-a"))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(listResourcesUseCase).list(new ListResourcesQuery(TenantId.of(TENANT_ID_VALUE)));
    }

    @Test
    void get_success_returnsTenantResource() throws Exception {
        when(getResourceUseCase.get(any(GetResourceQuery.class)))
                .thenReturn(resourceResult("Room A", "room-a", null, ResourceStatus.INACTIVE));

        mockMvc.perform(get("/api/resources/{resourceId}", RESOURCE_ID_VALUE).with(jwtPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.slug").value("room-a"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(getResourceUseCase)
                .get(
                        new GetResourceQuery(
                                TenantId.of(TENANT_ID_VALUE), ResourceId.of(RESOURCE_ID_VALUE)));
    }

    @Test
    void update_success_returnsUpdatedResourceAndTenantScopedCommand() throws Exception {
        when(updateResourceUseCase.update(any(UpdateResourceCommand.class)))
                .thenReturn(resourceResult("Room B", "room-b", "Updated", ResourceStatus.ACTIVE));

        mockMvc.perform(
                        put("/api/resources/{resourceId}", RESOURCE_ID_VALUE)
                                .with(jwtPrincipal())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "name": "Room B",
                                            "slug": "room-b",
                                            "description": "Updated"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Room B"))
                .andExpect(jsonPath("$.slug").value("room-b"))
                .andExpect(jsonPath("$.description").value("Updated"));

        final var captor = ArgumentCaptor.forClass(UpdateResourceCommand.class);
        verify(updateResourceUseCase).update(captor.capture());
        final var command = captor.getValue();
        Assertions.assertThat(command.tenantId().value()).isEqualTo(TENANT_ID_VALUE);
        Assertions.assertThat(command.resourceId().value()).isEqualTo(RESOURCE_ID_VALUE);
        Assertions.assertThat(command.name()).isEqualTo("Room B");
        Assertions.assertThat(command.slug()).isEqualTo("room-b");
        Assertions.assertThat(command.description()).isEqualTo("Updated");
    }

    @Test
    void delete_success_returns204AndTenantScopedCommand() throws Exception {
        mockMvc.perform(
                        delete("/api/resources/{resourceId}", RESOURCE_ID_VALUE)
                                .with(jwtPrincipal()))
                .andExpect(status().isNoContent());

        verify(deactivateResourceUseCase)
                .deactivate(
                        new DeactivateResourceCommand(
                                TenantId.of(TENANT_ID_VALUE), ResourceId.of(RESOURCE_ID_VALUE)));
    }

    @Test
    void duplicateSlug_returns409() throws Exception {
        when(createResourceUseCase.create(any()))
                .thenThrow(
                        new ResourceSlugAlreadyExistsException(
                                TenantId.of(TENANT_ID_VALUE), new ResourceSlug("room-a")));

        mockMvc.perform(
                        post("/api/resources")
                                .with(jwtPrincipal())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "name": "Room A",
                                            "slug": "room-a"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Resource slug 'room-a' is already in use for this tenant"));
    }

    @Test
    void missingResource_returns404() throws Exception {
        when(getResourceUseCase.get(any()))
                .thenThrow(
                        new ResourceNotFoundException(
                                TenantId.of(TENANT_ID_VALUE), ResourceId.of(RESOURCE_ID_VALUE)));

        mockMvc.perform(get("/api/resources/{resourceId}", RESOURCE_ID_VALUE).with(jwtPrincipal()))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Resource '%s' was not found".formatted(RESOURCE_ID_VALUE)));
    }

    @Test
    void invalidRequest_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/api/resources")
                                .with(jwtPrincipal())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "name": "",
                                            "slug": "aa",
                                            "description": "%s"
                                        }
                                        """
                                                .formatted("x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(3)));
    }

    private static ResourceResult resourceResult(
            final String name,
            final String slug,
            final String description,
            final ResourceStatus status) {
        return new ResourceResult(RESOURCE_ID_VALUE, name, slug, description, status, NOW, NOW);
    }

    private static RequestPostProcessor jwtPrincipal() {
        return request -> {
            final var jwt =
                    Jwt.withTokenValue("test-token")
                            .header("alg", "HS256")
                            .subject(ADMIN_ID_VALUE.toString())
                            .claim(USER_ID, ADMIN_ID_VALUE.toString())
                            .claim(TENANT_ID, TENANT_ID_VALUE.toString())
                            .claim(ROLE, RoleNames.OWNER)
                            .build();
            request.setUserPrincipal(new JwtAuthenticationToken(jwt, List.of()));
            return request;
        };
    }
}

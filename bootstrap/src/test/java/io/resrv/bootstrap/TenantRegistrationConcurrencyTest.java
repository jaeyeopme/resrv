package io.resrv.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TenantRegistrationConcurrencyTest extends AbstractIntegrationTest {

    @Test
    void concurrentSameSlug_exactlyOneSucceeds() throws Exception {
        final var slug = "concurrent-" + System.nanoTime() % 100000;
        final var jsonTemplate =
                """
                {
                    "name": "Concurrent %d",
                    "slug": "%s",
                    "timezone": "UTC",
                    "slotDuration": 30,
                    "admin": {
                        "email": "concurrent%d@example.com",
                        "password": "password123"
                    }
                }
                """;

        try (final var executor = Executors.newFixedThreadPool(2)) {
            final Future<Integer> future1 =
                    executor.submit(
                            () ->
                                    mockMvc.perform(
                                                    post("/api/tenants")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .content(
                                                                    jsonTemplate.formatted(
                                                                            1, slug, 1)))
                                            .andReturn()
                                            .getResponse()
                                            .getStatus());

            final Future<Integer> future2 =
                    executor.submit(
                            () ->
                                    mockMvc.perform(
                                                    post("/api/tenants")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .content(
                                                                    jsonTemplate.formatted(
                                                                            2, slug, 2)))
                                            .andReturn()
                                            .getResponse()
                                            .getStatus());

            final var status1 = future1.get();
            final var status2 = future2.get();

            final var statuses = List.of(status1, status2);
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        }
    }
}

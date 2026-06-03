package io.resrv.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TicketingDocumentationConsistencyTest {

    @Test
    void ticketingOperationsTestingAndSecurityDocsDescribeRuntimeAndRetention() throws Exception {
        final var operations = read("docs/operations.md");
        final var testing = read("docs/testing.md");
        final var security = read("docs/security.md");

        assertThat(operations)
                .contains("Ticketing is assembled into the platform runtime")
                .contains("no separate ticketing backend runtime")
                .contains("24 hours")
                .contains("30 days after replay expiry")
                .contains("purchase correctness does not depend on");
        assertThat(testing)
                .contains("./gradlew :platform:test --tests '*Ticketing*'")
                .contains("invalid_retry")
                .contains("expired_key")
                .contains("non-enumerating not-found responses");
        assertThat(security)
                .contains("Unavailable selected seats")
                .contains("`409 Conflict`")
                .contains("same not-found style public response")
                .contains("invalid_retry")
                .contains("expired_key");
    }

    private static String read(final String path) throws IOException {
        return Files.readString(Path.of("..").resolve(path));
    }
}

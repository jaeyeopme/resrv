package io.resrv.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///resrv",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "resrv.jwt.secret-key=01234567890123456789012345678901",
            "resrv.jwt.issuer=resrv-test",
            "resrv.jwt.audience=resrv-api",
            "resrv.jwt.expiration=3600",
            "resrv.security.password-reset.public-base-url=https://app.example.com",
            "resrv.security.password-reset.token-ttl=PT30M"
        })
@AutoConfigureMockMvc
@Import(FakePasswordResetEmailAdapter.class)
final class TicketPurchaseAttemptAbsenceIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void ticketingSchemaDoesNotContainCheckoutAttemptOrFailureLifecycleTables() {
        final var tableNames =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'ticketing'
                        """,
                        String.class);

        assertThat(tableNames)
                .doesNotContain(
                        "ticket_checkout",
                        "ticket_checkout_attempt",
                        "ticket_purchase_attempt",
                        "ticket_purchase_failure",
                        "ticket_cancellation",
                        "ticket_expiration");
    }
}

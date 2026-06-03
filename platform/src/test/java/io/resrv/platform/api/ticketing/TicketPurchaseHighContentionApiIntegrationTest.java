package io.resrv.platform.api;

import static io.resrv.platform.api.TicketingApiTestSupport.clean;
import static io.resrv.platform.api.TicketingApiTestSupport.insertAccount;
import static io.resrv.platform.api.TicketingApiTestSupport.insertBusiness;
import static io.resrv.platform.api.TicketingApiTestSupport.insertEvent;
import static io.resrv.platform.api.TicketingApiTestSupport.insertSeat;
import static io.resrv.platform.api.TicketingApiTestSupport.submitPurchase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
final class TicketPurchaseHighContentionApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clean(jdbcTemplate);
    }

    @Test
    void overlappingMultiSeatPurchasesDoNotCreatePartialOwnership() throws Exception {
        final var businessId = insertBusiness(jdbcTemplate, "ticketing-high-contention");
        final var eventId = insertEvent(jdbcTemplate, businessId);
        final var firstSeatId = insertSeat(jdbcTemplate, eventId, "A-1");
        final var secondSeatId = insertSeat(jdbcTemplate, eventId, "A-2");
        final var thirdSeatId = insertSeat(jdbcTemplate, eventId, "A-3");

        try (final var executor = Executors.newFixedThreadPool(6)) {
            final var futures =
                    IntStream.range(0, 12)
                            .mapToObj(
                                    index ->
                                            executor.submit(
                                                    () -> {
                                                        final var customerId =
                                                                insertAccount(
                                                                        jdbcTemplate,
                                                                        "mixed-"
                                                                                + index
                                                                                + "@example.com");
                                                        final var seats =
                                                                index % 2 == 0
                                                                        ? new UUID[] {
                                                                            firstSeatId,
                                                                            secondSeatId
                                                                        }
                                                                        : new UUID[] {
                                                                            secondSeatId,
                                                                            thirdSeatId
                                                                        };
                                                        return submitPurchase(
                                                                mockMvc,
                                                                customerId,
                                                                eventId,
                                                                "mixed-key-" + index,
                                                                seats);
                                                    }))
                            .toList();

            final var statuses = new ArrayList<Integer>();
            for (final var future : futures) {
                statuses.add(future.get());
            }

            assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(1);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    """
                                    SELECT COUNT(*)
                                    FROM ticketing.ticket_purchase_seat
                                    """,
                                    Integer.class))
                    .isEqualTo(2);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    """
                                    SELECT COUNT(*)
                                    FROM ticketing.ticket_purchase p
                                    WHERE NOT EXISTS (
                                        SELECT 1
                                        FROM ticketing.ticket_purchase_seat ps
                                        WHERE ps.ticket_purchase_id = p.id
                                    )
                                    """,
                                    Integer.class))
                    .isZero();
        }
    }
}

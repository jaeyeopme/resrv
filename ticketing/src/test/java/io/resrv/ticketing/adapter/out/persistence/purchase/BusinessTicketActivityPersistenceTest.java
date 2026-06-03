package io.resrv.ticketing.adapter.out.persistence.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.ticketing.domain.event.TicketEventId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BusinessTicketActivityPersistenceTest {

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void returnsNoPurchasesForWrongBusinessOrEvent() {
        final var adapter = new TicketPurchaseActivityPersistenceAdapter(jdbcTemplate);

        final var views =
                adapter.findBusinessEventPurchases(
                        BusinessId.of(UUID.randomUUID()), TicketEventId.of(UUID.randomUUID()));

        assertThat(views).isEmpty();
    }
}

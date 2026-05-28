package io.resrv.timeslot.adapter.out.persistence.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ReservationId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.reservation.out.ReservationCommandPort;
import io.resrv.timeslot.application.reservation.out.ReservationQueryPort;
import io.resrv.timeslot.domain.reservation.Reservation;
import io.resrv.timeslot.domain.reservation.ReservationState;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan("io.resrv.timeslot.adapter.out.persistence")
class ReservationPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-05-25T01:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-05-25T01:30:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private ReservationCommandPort commandPort;

    @Autowired private ReservationQueryPort queryPort;

    @Test
    void activeBlockerQueryMatchesDomainBlockerStates() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var activeHold =
                Reservation.hold(
                        businessId,
                        resourceId,
                        AccountId.create(),
                        START_AT,
                        END_AT,
                        NOW.plusSeconds(60),
                        NOW);
        final var confirmed =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW);
        final var checkedIn =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW)
                        .checkIn(START_AT);
        final var expiredHold =
                Reservation.hold(
                        businessId,
                        resourceId,
                        AccountId.create(),
                        START_AT,
                        END_AT,
                        NOW.minusSeconds(60),
                        NOW.minusSeconds(120));
        final var released =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .release(NOW);
        final var customerCancelled =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW)
                        .cancelByCustomer(NOW, START_AT);
        final var businessCancelled =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW)
                        .cancelByBusiness(NOW);
        final var noShow =
                Reservation.hold(
                                businessId,
                                resourceId,
                                AccountId.create(),
                                START_AT,
                                END_AT,
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW)
                        .markNoShow(END_AT);
        commandPort.save(activeHold);
        commandPort.save(confirmed);
        commandPort.save(checkedIn);
        commandPort.save(expiredHold);
        commandPort.save(released);
        commandPort.save(customerCancelled);
        commandPort.save(businessCancelled);
        commandPort.save(noShow);

        final var blockers =
                queryPort.findActiveBlockers(businessId, resourceId, START_AT, END_AT, NOW);

        assertEquals(
                Set.of(activeHold.id(), confirmed.id(), checkedIn.id()),
                blockers.stream().map(Reservation::id).collect(Collectors.toSet()));
        assertTrue(blockers.stream().allMatch(reservation -> reservation.blocksSlotAt(NOW)));
    }

    @Test
    void businessDateWindowQueryFiltersAndSortsReservations() {
        final var businessId = BusinessId.create();
        final var otherBusinessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var otherResourceId = ResourceId.create();
        final var customerId = AccountId.create();
        final var otherCustomerId = AccountId.create();
        final var later =
                Reservation.hold(
                                businessId,
                                resourceId,
                                customerId,
                                Instant.parse("2026-05-25T03:00:00Z"),
                                Instant.parse("2026-05-25T03:30:00Z"),
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW);
        final var earlier =
                Reservation.hold(
                                businessId,
                                resourceId,
                                customerId,
                                Instant.parse("2026-05-25T01:00:00Z"),
                                Instant.parse("2026-05-25T01:30:00Z"),
                                NOW.plusSeconds(60),
                                NOW)
                        .confirm(NOW);
        commandPort.save(later);
        commandPort.save(earlier);
        commandPort.save(
                Reservation.hold(
                        businessId,
                        otherResourceId,
                        customerId,
                        Instant.parse("2026-05-25T02:00:00Z"),
                        Instant.parse("2026-05-25T02:30:00Z"),
                        NOW.plusSeconds(60),
                        NOW));
        commandPort.save(
                Reservation.hold(
                        businessId,
                        resourceId,
                        otherCustomerId,
                        Instant.parse("2026-05-25T02:00:00Z"),
                        Instant.parse("2026-05-25T02:30:00Z"),
                        NOW.plusSeconds(60),
                        NOW));
        commandPort.save(
                Reservation.hold(
                        otherBusinessId,
                        resourceId,
                        customerId,
                        Instant.parse("2026-05-25T01:00:00Z"),
                        Instant.parse("2026-05-25T01:30:00Z"),
                        NOW.plusSeconds(60),
                        NOW));

        final var results =
                queryPort.findByBusinessDateWindow(
                        businessId,
                        Instant.parse("2026-05-25T00:00:00Z"),
                        Instant.parse("2026-05-26T00:00:00Z"),
                        resourceId,
                        customerId);

        assertEquals(2, results.size());
        assertEquals(earlier.id(), results.get(0).id());
        assertEquals(later.id(), results.get(1).id());
    }

    @Test
    void customerHistoryQueryFiltersOwnerSortsDescendingAndBoundsPages() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var accountId = AccountId.create();
        final var otherAccountId = AccountId.create();
        final var later = reservation(businessId, resourceId, accountId, 3, 1).confirm(NOW);
        final var earlier = reservation(businessId, resourceId, accountId, 1, 2).confirm(NOW);
        final var otherCustomer = reservation(businessId, resourceId, otherAccountId, 4, 3);
        commandPort.save(earlier);
        commandPort.save(later);
        commandPort.save(otherCustomer);

        final var results = queryPort.findByCustomerAccountId(accountId, 0, 1, null, null, NOW);

        assertEquals(2, results.totalElements());
        assertEquals(2, results.totalPages());
        assertEquals(List.of(later.id()), results.items().stream().map(Reservation::id).toList());

        final var secondPage = queryPort.findByCustomerAccountId(accountId, 1, 1, null, null, NOW);

        assertEquals(
                List.of(earlier.id()), secondPage.items().stream().map(Reservation::id).toList());
    }

    @Test
    void customerHistoryFiltersDerivedStateAndUpcomingBeforePagination() {
        final var businessId = BusinessId.create();
        final var resourceId = ResourceId.create();
        final var accountId = AccountId.create();
        final var expiredHold =
                Reservation.reconstitute(
                        ReservationId.of(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                        businessId,
                        resourceId,
                        accountId,
                        START_AT.plusSeconds(7200),
                        END_AT.plusSeconds(7200),
                        NOW.minusSeconds(1),
                        NOW.minusSeconds(10),
                        NOW.minusSeconds(10),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        final var confirmed = reservation(businessId, resourceId, accountId, 1, 2).confirm(NOW);
        commandPort.save(expiredHold);
        commandPort.save(confirmed);

        final var stateFiltered =
                queryPort.findByCustomerAccountId(
                        accountId, 0, 1, ReservationState.CONFIRMED, null, NOW);

        assertEquals(1, stateFiltered.totalElements());
        assertEquals(confirmed.id(), stateFiltered.items().getFirst().id());

        final var upcomingFiltered =
                queryPort.findByCustomerAccountId(accountId, 0, 1, null, true, NOW);

        assertEquals(1, upcomingFiltered.totalElements());
        assertEquals(confirmed.id(), upcomingFiltered.items().getFirst().id());
    }

    private static Reservation reservation(
            final BusinessId businessId,
            final ResourceId resourceId,
            final AccountId accountId,
            final int startHour,
            final int idSuffix) {
        final var startAt = Instant.parse("2026-05-25T00:00:00Z").plusSeconds(startHour * 3600L);
        return Reservation.reconstitute(
                ReservationId.of(
                        UUID.fromString("00000000-0000-0000-0000-%012d".formatted(idSuffix))),
                businessId,
                resourceId,
                accountId,
                startAt,
                startAt.plusSeconds(1800),
                NOW.plusSeconds(3600),
                NOW.plusSeconds(idSuffix),
                NOW.plusSeconds(idSuffix),
                null,
                null,
                null,
                null,
                null,
                null);
    }
}

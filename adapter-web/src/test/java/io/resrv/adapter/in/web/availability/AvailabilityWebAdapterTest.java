package io.resrv.adapter.in.web.availability;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.availability.in.DateAvailabilityOverrideResult;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.DeleteDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.DeleteWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideCommand;
import io.resrv.application.availability.in.UpsertDateAvailabilityOverrideUseCase;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityCommand;
import io.resrv.application.availability.in.UpsertWeeklyAvailabilityUseCase;
import io.resrv.application.availability.in.WeeklyAvailabilityResult;
import io.resrv.application.reservation.in.ListAvailableSlotsQuery;
import io.resrv.application.reservation.in.ListAvailableSlotsUseCase;
import io.resrv.application.reservation.in.ListResourceReservationsQuery;
import io.resrv.application.reservation.in.ListResourceReservationsUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.application.reservation.in.SlotResult;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AvailabilityWebAdapter.class)
class AvailabilityWebAdapterTest {

    private static final UUID TENANT_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESOURCE_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID WEEKLY_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID OVERRIDE_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID RESERVATION_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final LocalDate DATE = LocalDate.parse("2025-01-02");
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2025-01-02T09:00:00Z");
    private static final Instant END_AT = Instant.parse("2025-01-02T09:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UpsertWeeklyAvailabilityUseCase upsertWeeklyAvailabilityUseCase;

    @MockitoBean private DeleteWeeklyAvailabilityUseCase deleteWeeklyAvailabilityUseCase;

    @MockitoBean
    private UpsertDateAvailabilityOverrideUseCase upsertDateAvailabilityOverrideUseCase;

    @MockitoBean
    private DeleteDateAvailabilityOverrideUseCase deleteDateAvailabilityOverrideUseCase;

    @MockitoBean private ListAvailableSlotsUseCase listAvailableSlotsUseCase;

    @MockitoBean private ListResourceReservationsUseCase listResourceReservationsUseCase;

    @Test
    void upsertWeekly_success_returnsAvailabilityAndTenantScopedCommand() throws Exception {
        when(upsertWeeklyAvailabilityUseCase.upsert(any(UpsertWeeklyAvailabilityCommand.class)))
                .thenReturn(weeklyAvailabilityResult());

        mockMvc.perform(
                        put(
                                        "/api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                                        RESOURCE_ID_VALUE,
                                        1)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "startTime": "09:00:00",
                                            "endTime": "17:00:00"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(WEEKLY_ID_VALUE.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID_VALUE.toString()))
                .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("17:00:00"));

        verify(upsertWeeklyAvailabilityUseCase)
                .upsert(
                        new UpsertWeeklyAvailabilityCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DayOfWeek.MONDAY,
                                LocalTime.parse("09:00:00"),
                                LocalTime.parse("17:00:00")));
    }

    @Test
    void deleteWeekly_success_returns204AndTenantScopedCommand() throws Exception {
        mockMvc.perform(
                        delete(
                                        "/api/resources/{resourceId}/weekly-availability/{dayOfWeek}",
                                        RESOURCE_ID_VALUE,
                                        1)
                                .with(jwtPrincipal(RoleNames.STAFF, ADMIN_ID_VALUE)))
                .andExpect(status().isNoContent());

        verify(deleteWeeklyAvailabilityUseCase)
                .delete(
                        new DeleteWeeklyAvailabilityCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DayOfWeek.MONDAY));
    }

    @Test
    void upsertDateOverride_success_returnsOverrideAndTenantScopedCommand() throws Exception {
        when(upsertDateAvailabilityOverrideUseCase.upsert(
                        any(UpsertDateAvailabilityOverrideCommand.class)))
                .thenReturn(dateAvailabilityOverrideResult());

        mockMvc.perform(
                        put(
                                        "/api/resources/{resourceId}/availability-exceptions/{date}",
                                        RESOURCE_ID_VALUE,
                                        DATE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "closed": false,
                                            "startTime": "10:00:00",
                                            "endTime": "15:00:00"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OVERRIDE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID_VALUE.toString()))
                .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.date").value("2025-01-02"))
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.endTime").value("15:00:00"));

        verify(upsertDateAvailabilityOverrideUseCase)
                .upsert(
                        new UpsertDateAvailabilityOverrideCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DATE,
                                false,
                                LocalTime.parse("10:00:00"),
                                LocalTime.parse("15:00:00")));
    }

    @Test
    void deleteDateOverride_success_returns204AndTenantScopedCommand() throws Exception {
        mockMvc.perform(
                        delete(
                                        "/api/resources/{resourceId}/availability-exceptions/{date}",
                                        RESOURCE_ID_VALUE,
                                        DATE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isNoContent());

        verify(deleteDateAvailabilityOverrideUseCase)
                .delete(
                        new DeleteDateAvailabilityOverrideCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DATE));
    }

    @Test
    void listSlots_success_returnsTenantScopedSlots() throws Exception {
        when(listAvailableSlotsUseCase.listAvailableSlots(any(ListAvailableSlotsQuery.class)))
                .thenReturn(List.of(new SlotResult(START_AT, END_AT)));

        mockMvc.perform(
                        get("/api/resources/{resourceId}/slots", RESOURCE_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE))
                                .queryParam("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value("2025-01-02T09:00:00Z"))
                .andExpect(jsonPath("$[0].endAt").value("2025-01-02T09:30:00Z"));

        verify(listAvailableSlotsUseCase)
                .listAvailableSlots(
                        new ListAvailableSlotsQuery(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DATE));
    }

    @Test
    void listResourceReservations_success_returnsTenantScopedReservations() throws Exception {
        when(listResourceReservationsUseCase.listResourceReservations(
                        any(ListResourceReservationsQuery.class)))
                .thenReturn(List.of(reservationResult(ReservationStatus.CONFIRMED)));

        mockMvc.perform(
                        get("/api/resources/{resourceId}/reservations", RESOURCE_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE))
                                .queryParam("date", DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].confirmedAt").value("2025-01-01T00:00:00Z"));

        verify(listResourceReservationsUseCase)
                .listResourceReservations(
                        new ListResourceReservationsQuery(
                                TenantId.of(TENANT_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                DATE));
    }

    private static WeeklyAvailabilityResult weeklyAvailabilityResult() {
        return new WeeklyAvailabilityResult(
                WEEKLY_ID_VALUE,
                TENANT_ID_VALUE,
                RESOURCE_ID_VALUE,
                DayOfWeek.MONDAY,
                LocalTime.parse("09:00:00"),
                LocalTime.parse("17:00:00"),
                NOW,
                NOW);
    }

    private static DateAvailabilityOverrideResult dateAvailabilityOverrideResult() {
        return new DateAvailabilityOverrideResult(
                OVERRIDE_ID_VALUE,
                TENANT_ID_VALUE,
                RESOURCE_ID_VALUE,
                DATE,
                false,
                LocalTime.parse("10:00:00"),
                LocalTime.parse("15:00:00"),
                NOW,
                NOW);
    }

    private static ReservationResult reservationResult(final ReservationStatus status) {
        return new ReservationResult(
                RESERVATION_ID_VALUE,
                TENANT_ID_VALUE,
                RESOURCE_ID_VALUE,
                CUSTOMER_ID_VALUE,
                START_AT,
                END_AT,
                status,
                null,
                NOW,
                NOW,
                NOW,
                null);
    }

    private static RequestPostProcessor jwtPrincipal(final String role, final UUID userId) {
        return request -> {
            final var jwt =
                    Jwt.withTokenValue("test-token")
                            .header("alg", "HS256")
                            .subject(userId.toString())
                            .claim(USER_ID, userId.toString())
                            .claim(TENANT_ID, TENANT_ID_VALUE.toString())
                            .claim(ROLE, role)
                            .build();
            request.setUserPrincipal(new JwtAuthenticationToken(jwt, List.of()));
            return request;
        };
    }
}

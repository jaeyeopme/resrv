package io.resrv.adapter.in.web.reservation;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.reservation.in.AdminCancelReservationCommand;
import io.resrv.application.reservation.in.AdminCancelReservationUseCase;
import io.resrv.application.reservation.in.CheckInReservationCommand;
import io.resrv.application.reservation.in.CheckInReservationUseCase;
import io.resrv.application.reservation.in.ListAdminReservationsQuery;
import io.resrv.application.reservation.in.ListAdminReservationsUseCase;
import io.resrv.application.reservation.in.MarkNoShowReservationCommand;
import io.resrv.application.reservation.in.MarkNoShowReservationUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AdminReservationWebAdapter.class)
class AdminReservationWebAdapterTest {

    private static final UUID TENANT_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESOURCE_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID RESERVATION_ID_VALUE =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2025-01-02T09:00:00Z");
    private static final Instant END_AT = Instant.parse("2025-01-02T09:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ListAdminReservationsUseCase listAdminReservationsUseCase;

    @MockitoBean private AdminCancelReservationUseCase adminCancelReservationUseCase;

    @MockitoBean private CheckInReservationUseCase checkInReservationUseCase;

    @MockitoBean private MarkNoShowReservationUseCase markNoShowReservationUseCase;

    @Test
    void list_success_returnsAdminScopedReservations() throws Exception {
        when(listAdminReservationsUseCase.listAdminReservations(
                        any(ListAdminReservationsQuery.class)))
                .thenReturn(List.of(reservationResult(ReservationStatus.CONFIRMED)));

        mockMvc.perform(
                        get("/api/reservations")
                                .param("date", "2025-01-02")
                                .param("resourceId", RESOURCE_ID_VALUE.toString())
                                .param("customerId", CUSTOMER_ID_VALUE.toString())
                                .param("status", "CONFIRMED")
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        verify(listAdminReservationsUseCase)
                .listAdminReservations(any(ListAdminReservationsQuery.class));
    }

    @Test
    void list_customerRoleReturns403BeforeUseCase() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .param("date", "2025-01-02")
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Admin role is required"));

        verifyNoInteractions(listAdminReservationsUseCase);
    }

    @Test
    void list_missingDateReturns400() throws Exception {
        mockMvc.perform(
                        get("/api/reservations")
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listAdminReservationsUseCase);
    }

    @Test
    void adminCancel_successUsesTenantScopedCommand() throws Exception {
        when(adminCancelReservationUseCase.adminCancel(any(AdminCancelReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.ADMIN_CANCELLED));

        mockMvc.perform(
                        post("/api/reservations/{reservationId}/admin-cancel", RESERVATION_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_CANCELLED"));

        verify(adminCancelReservationUseCase)
                .adminCancel(
                        new AdminCancelReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ReservationId.of(RESERVATION_ID_VALUE)));
    }

    @Test
    void checkIn_successUsesTenantScopedCommand() throws Exception {
        when(checkInReservationUseCase.checkIn(any(CheckInReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.CHECKED_IN));

        mockMvc.perform(
                        post("/api/reservations/{reservationId}/check-in", RESERVATION_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        verify(checkInReservationUseCase)
                .checkIn(
                        new CheckInReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ReservationId.of(RESERVATION_ID_VALUE)));
    }

    @Test
    void markNoShow_successUsesTenantScopedCommand() throws Exception {
        when(markNoShowReservationUseCase.markNoShow(any(MarkNoShowReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.NO_SHOW));

        mockMvc.perform(
                        post("/api/reservations/{reservationId}/no-show", RESERVATION_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        verify(markNoShowReservationUseCase)
                .markNoShow(
                        new MarkNoShowReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                ReservationId.of(RESERVATION_ID_VALUE)));
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
                status == ReservationStatus.ADMIN_CANCELLED ? NOW : null);
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

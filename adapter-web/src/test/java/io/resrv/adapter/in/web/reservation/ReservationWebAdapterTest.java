package io.resrv.adapter.in.web.reservation;

import static io.resrv.application.auth.TokenClaimNames.ROLE;
import static io.resrv.application.auth.TokenClaimNames.TENANT_ID;
import static io.resrv.application.auth.TokenClaimNames.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.resrv.application.auth.RoleNames;
import io.resrv.application.reservation.in.CancelCustomerReservationCommand;
import io.resrv.application.reservation.in.CancelCustomerReservationUseCase;
import io.resrv.application.reservation.in.ConfirmReservationCommand;
import io.resrv.application.reservation.in.ConfirmReservationUseCase;
import io.resrv.application.reservation.in.HoldReservationCommand;
import io.resrv.application.reservation.in.HoldReservationUseCase;
import io.resrv.application.reservation.in.ListCustomerReservationsQuery;
import io.resrv.application.reservation.in.ListCustomerReservationsUseCase;
import io.resrv.application.reservation.in.ReservationResult;
import io.resrv.domain.customer.CustomerId;
import io.resrv.domain.reservation.ReservationId;
import io.resrv.domain.reservation.ReservationStatus;
import io.resrv.domain.resource.ResourceId;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
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

@WebMvcTest(ReservationWebAdapter.class)
class ReservationWebAdapterTest {

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
    private static final Instant HOLD_EXPIRES_AT = Instant.parse("2025-01-01T00:10:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private HoldReservationUseCase holdReservationUseCase;

    @MockitoBean private ConfirmReservationUseCase confirmReservationUseCase;

    @MockitoBean private ListCustomerReservationsUseCase listCustomerReservationsUseCase;

    @MockitoBean private CancelCustomerReservationUseCase cancelCustomerReservationUseCase;

    @Test
    void hold_success_returns201WithLocationAndCustomerScopedCommand() throws Exception {
        when(holdReservationUseCase.hold(any(HoldReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.HELD));

        mockMvc.perform(
                        post("/api/reservation-holds")
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "resourceId": "%s",
                                            "startAt": "2025-01-02T09:00:00Z"
                                        }
                                        """
                                                .formatted(RESOURCE_ID_VALUE)))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string("Location", "/api/me/reservations/" + RESERVATION_ID_VALUE))
                .andExpect(jsonPath("$.id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID_VALUE.toString()))
                .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID_VALUE.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID_VALUE.toString()))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.holdExpiresAt").value("2025-01-01T00:10:00Z"));

        verify(holdReservationUseCase)
                .hold(
                        new HoldReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                CustomerId.of(CUSTOMER_ID_VALUE),
                                ResourceId.of(RESOURCE_ID_VALUE),
                                START_AT));
    }

    @Test
    void confirm_success_returnsConfirmedReservation() throws Exception {
        when(confirmReservationUseCase.confirm(any(ConfirmReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.CONFIRMED));

        mockMvc.perform(
                        post("/api/reservation-holds/{reservationId}/confirm", RESERVATION_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").value("2025-01-01T00:00:00Z"));

        verify(confirmReservationUseCase)
                .confirm(
                        new ConfirmReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                CustomerId.of(CUSTOMER_ID_VALUE),
                                ReservationId.of(RESERVATION_ID_VALUE)));
    }

    @Test
    void listMine_success_returnsCustomerScopedReservations() throws Exception {
        when(listCustomerReservationsUseCase.listCustomerReservations(
                        any(ListCustomerReservationsQuery.class)))
                .thenReturn(List.of(reservationResult(ReservationStatus.CONFIRMED)));

        mockMvc.perform(
                        get("/api/me/reservations")
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        verify(listCustomerReservationsUseCase)
                .listCustomerReservations(
                        new ListCustomerReservationsQuery(
                                TenantId.of(TENANT_ID_VALUE), CustomerId.of(CUSTOMER_ID_VALUE)));
    }

    @Test
    void cancel_success_returnsCancelledReservation() throws Exception {
        when(cancelCustomerReservationUseCase.cancel(any(CancelCustomerReservationCommand.class)))
                .thenReturn(reservationResult(ReservationStatus.CUSTOMER_CANCELLED));

        mockMvc.perform(
                        post("/api/me/reservations/{reservationId}/cancel", RESERVATION_ID_VALUE)
                                .with(jwtPrincipal(RoleNames.CUSTOMER, CUSTOMER_ID_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVATION_ID_VALUE.toString()))
                .andExpect(jsonPath("$.status").value("CUSTOMER_CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2025-01-01T00:00:00Z"));

        verify(cancelCustomerReservationUseCase)
                .cancel(
                        new CancelCustomerReservationCommand(
                                TenantId.of(TENANT_ID_VALUE),
                                CustomerId.of(CUSTOMER_ID_VALUE),
                                ReservationId.of(RESERVATION_ID_VALUE)));
    }

    @Test
    void hold_withAdminRole_returns403BeforeUseCase() throws Exception {
        mockMvc.perform(
                        post("/api/reservation-holds")
                                .with(jwtPrincipal(RoleNames.OWNER, ADMIN_ID_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "resourceId": "%s",
                                            "startAt": "2025-01-02T09:00:00Z"
                                        }
                                        """
                                                .formatted(RESOURCE_ID_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Customer role is required"));
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
                status == ReservationStatus.HELD ? HOLD_EXPIRES_AT : null,
                NOW,
                NOW,
                status == ReservationStatus.CONFIRMED ? NOW : null,
                status == ReservationStatus.CUSTOMER_CANCELLED ? NOW : null);
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

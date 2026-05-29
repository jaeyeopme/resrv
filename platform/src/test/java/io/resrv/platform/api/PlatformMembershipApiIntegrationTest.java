package io.resrv.platform.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
final class PlatformMembershipApiIntegrationTest {

    private static final String JWT_SECRET = "01234567890123456789012345678901";
    private static final String JWT_ISSUER = "resrv-test";
    private static final String JWT_AUDIENCE = "resrv-api";
    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM platform.sign_in_attempt");
        jdbcTemplate.update("DELETE FROM platform.password_reset_challenge");
        jdbcTemplate.update("DELETE FROM platform.account_sign_in_protection");
        jdbcTemplate.update("DELETE FROM platform.business_membership_audit_entry");
        jdbcTemplate.update("DELETE FROM platform.business_membership");
        jdbcTemplate.update("DELETE FROM platform.business");
        jdbcTemplate.update("DELETE FROM platform.account");
    }

    @Test
    void ownerCanGrantStaffListMembershipsAndAuditHistory() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var businessId = insertBusiness("membership-business");
        insertMembership(ownerId, businessId, "OWNER", true);

        final var grantResponse =
                mockMvc.perform(
                                post("/api/businesses/{businessId}/memberships", businessId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"targetAccountEmail":"staff@example.com"}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.membershipId", notNullValue()))
                        .andExpect(jsonPath("$.businessId").value(businessId.toString()))
                        .andExpect(jsonPath("$.account.accountId").value(staffId.toString()))
                        .andExpect(jsonPath("$.account.email").value("staff@example.com"))
                        .andExpect(jsonPath("$.role").value("STAFF"))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final String membershipId = JsonPath.read(grantResponse, "$.membershipId");

        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].membershipId").value(membershipId))
                .andExpect(jsonPath("$[1].account.email").value("staff@example.com"));

        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships/audit", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("GRANTED"))
                .andExpect(jsonPath("$[0].actor.email").value("owner@example.com"))
                .andExpect(jsonPath("$[0].target.email").value("staff@example.com"))
                .andExpect(jsonPath("$[0].previousState").doesNotExist())
                .andExpect(jsonPath("$[0].newState.role").value("STAFF"));
    }

    @Test
    void membershipAdministrationRejectsNonOwnerTargetFailureAndDuplicateActive() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var otherId = insertAccount("other@example.com", "Other", "ACTIVE");
        insertAccount("disabled@example.com", "Disabled", "DISABLED");
        final var businessId = insertBusiness("membership-denials");
        final var inactiveBusinessId = insertBusiness("membership-denials-inactive", "INACTIVE");
        insertMembership(ownerId, businessId, "OWNER", true);
        insertMembership(staffId, businessId, "STAFF", true);
        insertMembership(ownerId, inactiveBusinessId, "OWNER", true);
        final var staffMembershipId = membershipId(staffId, businessId);

        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(staffId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"other@example.com\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(staffId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships/audit", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(staffId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/memberships/{membershipId}",
                                        businessId,
                                        staffMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(staffId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        staffMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(staffId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(otherId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"staff@example.com\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(otherId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", inactiveBusinessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"staff@example.com\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships", inactiveBusinessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"missing@example.com\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"disabled@example.com\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"staff@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanUpdateRoleAndDisableMembershipWithLastOwnerProtection() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var businessId = insertBusiness("membership-updates");
        final var ownerMembershipId = insertMembership(ownerId, businessId, "OWNER", true);
        final var staffMembershipId = insertMembership(staffId, businessId, "STAFF", true);

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/memberships/{membershipId}",
                                        businessId,
                                        staffMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        staffMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.disabledAt", notNullValue()));
        final var auditCountAfterDisable = auditCount(businessId);

        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        staffMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertEquals(auditCountAfterDisable, auditCount(businessId));

        final var auditResponse =
                mockMvc.perform(
                                get("/api/businesses/{businessId}/memberships/audit", businessId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final List<?> roleChangePreviousRoles =
                JsonPath.read(auditResponse, "$[?(@.action == 'ROLE_CHANGED')].previousState.role");
        final List<?> roleChangeNewRoles =
                JsonPath.read(auditResponse, "$[?(@.action == 'ROLE_CHANGED')].newState.role");
        final List<?> disabledPreviousActive =
                JsonPath.read(auditResponse, "$[?(@.action == 'DISABLED')].previousState.active");
        final List<?> disabledNewActive =
                JsonPath.read(auditResponse, "$[?(@.action == 'DISABLED')].newState.active");
        assertEquals(List.of("STAFF"), roleChangePreviousRoles);
        assertEquals(List.of("OWNER"), roleChangeNewRoles);
        assertEquals(List.of(true), disabledPreviousActive);
        assertEquals(List.of(false), disabledNewActive);

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/memberships/{membershipId}",
                                        businessId,
                                        ownerMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"STAFF\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        ownerMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanReactivateDisabledMembershipAndAuditIt() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var businessId = insertBusiness("membership-reactivation");
        insertMembership(ownerId, businessId, "OWNER", true);
        final var disabledMembershipId = insertMembership(staffId, businessId, "STAFF", false);

        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"staff@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").value(disabledMembershipId.toString()))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(
                        get("/api/businesses/{businessId}/memberships/audit", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REACTIVATED"))
                .andExpect(jsonPath("$[0].previousState.role").value("STAFF"))
                .andExpect(jsonPath("$[0].previousState.active").value(false))
                .andExpect(jsonPath("$[0].newState.role").value("STAFF"))
                .andExpect(jsonPath("$[0].newState.active").value(true));
    }

    @Test
    void wrongBusinessMembershipReturnsNotFoundAndOpenApiIncludesMembershipPaths()
            throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var businessId = insertBusiness("membership-wrong-business");
        final var otherBusinessId = insertBusiness("membership-other-business");
        insertMembership(ownerId, businessId, "OWNER", true);
        final var otherMembershipId = insertMembership(staffId, otherBusinessId, "STAFF", true);

        mockMvc.perform(
                        put(
                                        "/api/businesses/{businessId}/memberships/{membershipId}",
                                        businessId,
                                        otherMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        otherMembershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.summary")
                                .value("Grant staff membership"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.responses['201'].description")
                                .value("Membership granted"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.responses['400'].description")
                                .value("Target account is unavailable"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.responses['404'].description")
                                .value("Owner access is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".post.responses['409'].description")
                                .value("Active membership already exists"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".get.summary")
                                .value("List business memberships"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".get.responses['200'].description")
                                .value("Memberships returned"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".get.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships']"
                                                + ".get.responses['404'].description")
                                .value("Owner access is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/audit']"
                                                + ".get.summary")
                                .value("List membership audit history"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/audit']"
                                                + ".get.responses['200'].description")
                                .value("Audit history returned"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/audit']"
                                                + ".get.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/audit']"
                                                + ".get.responses['404'].description")
                                .value("Owner access is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.summary")
                                .value("Update membership role"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.responses['200'].description")
                                .value("Membership role updated"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.responses['400'].description")
                                .value("Requested role is invalid"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.responses['404'].description")
                                .value("Owner access or membership not found"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}']"
                                                + ".put.responses['409'].description")
                                .value("Last owner membership is protected"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}/disable']"
                                                + ".post.responses['200'].description")
                                .value("Membership disabled or current inactive state returned"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}/disable']"
                                                + ".post.responses['401'].description")
                                .value("Authentication is required"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}/disable']"
                                                + ".post.responses['404'].description")
                                .value("Owner access or membership not found"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/businesses/{businessId}/memberships/{membershipId}/disable']"
                                                + ".post.responses['409'].description")
                                .value("Last owner membership is protected"));
    }

    @Test
    void grantedAndDisabledMembershipAffectsRequestTimeBusinessAccessState() throws Exception {
        final var ownerId = insertAccount("owner@example.com", "Owner", "ACTIVE");
        final var staffId = insertAccount("staff@example.com", "Staff", "ACTIVE");
        final var businessId = insertBusiness("membership-access-state");
        insertMembership(ownerId, businessId, "OWNER", true);

        mockMvc.perform(
                        post("/api/businesses/{businessId}/memberships", businessId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"targetAccountEmail\":\"staff@example.com\"}"))
                .andExpect(status().isCreated());
        assertTrue(activeMembershipExists(staffId, businessId));

        final var membershipId = membershipId(staffId, businessId);
        mockMvc.perform(
                        post(
                                        "/api/businesses/{businessId}/memberships/{membershipId}/disable",
                                        businessId,
                                        membershipId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk());
        assertFalse(activeMembershipExists(staffId, businessId));
    }

    private UUID insertAccount(final String email, final String name, final String status) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.account
                    (id, email, name, hashed_password, status, created_at)
                VALUES (?, ?, ?, '$argon2id$test', ?, ?)
                """,
                id,
                email,
                name,
                status,
                Timestamp.from(NOW));
        return id;
    }

    private UUID insertBusiness(final String slug) {
        return insertBusiness(slug, "ACTIVE");
    }

    private UUID insertBusiness(final String slug, final String status) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.business
                    (id, name, slug, timezone, status, created_at)
                VALUES (?, 'Studio', ?, 'Asia/Seoul', ?, ?)
                """,
                id,
                slug,
                status,
                Timestamp.from(NOW));
        return id;
    }

    private UUID insertMembership(
            final UUID accountId, final UUID businessId, final String role, final boolean active) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.business_membership
                    (id, account_id, business_id, role, active, created_at, updated_at, disabled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                accountId,
                businessId,
                role,
                active,
                Timestamp.from(NOW),
                Timestamp.from(NOW),
                active ? null : Timestamp.from(NOW));
        return id;
    }

    private long auditCount(final UUID businessId) {
        final var count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) FROM platform.business_membership_audit_entry
                        WHERE business_id = ?
                        """,
                        Long.class,
                        businessId);
        return count == null ? 0 : count;
    }

    private UUID membershipId(final UUID accountId, final UUID businessId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM platform.business_membership
                WHERE account_id = ? AND business_id = ?
                """,
                UUID.class,
                accountId,
                businessId);
    }

    private boolean activeMembershipExists(final UUID accountId, final UUID businessId) {
        final var count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) FROM platform.business_membership
                        WHERE account_id = ? AND business_id = ? AND active = true
                        """,
                        Long.class,
                        accountId,
                        businessId);
        return count != null && count > 0;
    }

    private static String bearer(final UUID accountId) throws JOSEException {
        return "Bearer " + signedToken(accountId.toString());
    }

    private static String signedToken(final String accountId) throws JOSEException {
        final var now = Instant.now();
        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(JWT_ISSUER)
                        .subject(accountId)
                        .audience(List.of(JWT_AUDIENCE))
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(3600)))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("accountId", accountId)
                        .build();

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
    }
}

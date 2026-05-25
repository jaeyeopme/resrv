package io.resrv.timeslot.api.platform;

import io.resrv.shared.kernel.AccountId;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import io.resrv.timeslot.application.auth.out.BusinessAccessPort;
import io.resrv.timeslot.application.business.out.BusinessLookupPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PlatformBusinessLookupAdapter implements BusinessLookupPort, BusinessAccessPort {

    private final JdbcClient jdbcClient;

    PlatformBusinessLookupAdapter(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<BusinessView> findActiveById(final BusinessId businessId) {
        return jdbcClient
                .sql(
                        """
                        SELECT id, name, slug, timezone
                        FROM platform.business
                        WHERE id = :businessId
                          AND status = 'ACTIVE'
                        """)
                .param("businessId", businessId.value())
                .query(
                        (rs, rowNum) ->
                                new BusinessView(
                                        BusinessId.of(rs.getObject("id", UUID.class)),
                                        rs.getString("name"),
                                        rs.getString("slug"),
                                        Timezone.of(rs.getString("timezone"))))
                .optional();
    }

    @Override
    public boolean hasBusinessAccess(final AccountId accountId, final BusinessId businessId) {
        return jdbcClient
                .sql(
                        """
                        SELECT 1
                        FROM platform.business_membership
                        WHERE account_id = :accountId
                          AND business_id = :businessId
                          AND active = true
                          AND role IN ('OWNER', 'STAFF')
                        """)
                .param("accountId", accountId.value())
                .param("businessId", businessId.value())
                .query(Integer.class)
                .optional()
                .isPresent();
    }
}

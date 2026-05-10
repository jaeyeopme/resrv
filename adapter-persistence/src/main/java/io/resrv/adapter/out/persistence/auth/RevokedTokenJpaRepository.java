package io.resrv.adapter.out.persistence.auth;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface RevokedTokenJpaRepository extends CrudRepository<RevokedTokenJpaEntity, String> {

    boolean existsByJtiAndExpiresAtAfter(String jti, Instant now);

    long deleteByExpiresAtBefore(Instant now);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO revoked_token (jti, expires_at, revoked_at)
                    VALUES (:jti, :expiresAt, :revokedAt)
                    ON CONFLICT (jti) DO UPDATE
                    SET expires_at = GREATEST(revoked_token.expires_at, EXCLUDED.expires_at),
                        revoked_at = LEAST(revoked_token.revoked_at, EXCLUDED.revoked_at)
                    """,
            nativeQuery = true)
    void upsert(
            @Param("jti") String jti,
            @Param("expiresAt") Instant expiresAt,
            @Param("revokedAt") Instant revokedAt);
}

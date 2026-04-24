package io.resrv.bootstrap.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.resrv.application.auth.TokenClaimNames;
import io.resrv.application.auth.out.TokenGenerationPort;
import io.resrv.application.auth.out.TokenResult;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JwtTokenAdapter implements TokenGenerationPort {

    private final MACSigner signer;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenAdapter(final JwtProperties properties, final Clock clock) {
        try {
            this.signer = new MACSigner(properties.secretKey());
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Failed to initialize JWT signer", exception);
        }
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public TokenResult generate(final UUID userId, final UUID tenantId, final String role) {
        final var now = clock.instant();
        final var expiration = now.plusSeconds(properties.expiration());

        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(properties.issuer())
                        .subject(userId.toString())
                        .audience(List.of(properties.audience()))
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(expiration))
                        .jwtID(UUID.randomUUID().toString())
                        .claim(TokenClaimNames.USER_ID, userId.toString())
                        .claim(TokenClaimNames.TENANT_ID, tenantId.toString())
                        .claim(TokenClaimNames.ROLE, role)
                        .build();

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(signer);
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }

        return new TokenResult(signedJwt.serialize(), properties.expiration());
    }
}

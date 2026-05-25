package io.resrv.platform.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.out.TokenGenerationPort;
import io.resrv.shared.kernel.AccountId;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JwtTokenAdapter implements TokenGenerationPort {

    private final MACSigner signer;
    private final PlatformSecurityConfig.JwtProperties properties;
    private final Clock clock;

    JwtTokenAdapter(final PlatformSecurityConfig.JwtProperties properties, final Clock clock) {
        try {
            this.signer = new MACSigner(properties.secretKey());
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Failed to initialize JWT signer", exception);
        }
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public LoginResult generate(final AccountId accountId) {
        final var now = clock.instant();
        final var expiration = now.plusSeconds(properties.expiration());
        final var accountIdValue = accountId.value().toString();
        final var jwtId = UUID.randomUUID().toString();

        final var claims =
                new JWTClaimsSet.Builder()
                        .issuer(properties.issuer())
                        .subject(accountIdValue)
                        .audience(List.of(properties.audience()))
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(expiration))
                        .jwtID(jwtId)
                        .claim("accountId", accountIdValue)
                        .build();

        final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(signer);
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }

        return new LoginResult(signedJwt.serialize(), properties.expiration());
    }
}

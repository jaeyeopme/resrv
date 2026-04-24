package io.resrv.bootstrap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resrv.jwt")
record JwtProperties(String secretKey, String issuer, String audience, long expiration) {}

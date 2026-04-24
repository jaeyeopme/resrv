package io.resrv.bootstrap.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

final class JtiBlacklist {

    private final Cache<String, Instant> cache;

    JtiBlacklist(final Clock clock) {
        this.cache =
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfter(
                                new Expiry<String, Instant>() {
                                    @Override
                                    public long expireAfterCreate(
                                            final String key,
                                            final Instant expiration,
                                            final long currentTime) {
                                        final var ttl =
                                                Duration.between(clock.instant(), expiration);
                                        return ttl.isNegative() ? 0 : ttl.toNanos();
                                    }

                                    @Override
                                    public long expireAfterUpdate(
                                            final String key,
                                            final Instant expiration,
                                            final long currentTime,
                                            final long currentDuration) {
                                        return currentDuration;
                                    }

                                    @Override
                                    public long expireAfterRead(
                                            final String key,
                                            final Instant expiration,
                                            final long currentTime,
                                            final long currentDuration) {
                                        return currentDuration;
                                    }
                                })
                        .build();
    }

    void add(final String jti, final Instant expiration) {
        cache.put(jti, expiration);
    }

    boolean contains(final String jti) {
        return cache.getIfPresent(jti) != null;
    }
}

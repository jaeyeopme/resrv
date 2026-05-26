package io.resrv.platform.api;

import io.resrv.platform.application.auth.out.PasswordResetEmailPort;
import io.resrv.platform.domain.account.AccountEmail;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class FakePasswordResetEmailAdapter {

    @Bean
    @Primary
    FakePasswordResetEmailPort fakePasswordResetEmailPort() {
        return new FakePasswordResetEmailPort();
    }

    static final class FakePasswordResetEmailPort implements PasswordResetEmailPort {

        private final List<Delivery> deliveries = new ArrayList<>();

        @Override
        public void sendPasswordResetEmail(
                final AccountEmail recipient, final String resetLink, final Instant expiresAt) {
            deliveries.add(new Delivery(recipient.value(), resetLink, expiresAt));
        }

        List<Delivery> deliveries() {
            return List.copyOf(deliveries);
        }

        void clear() {
            deliveries.clear();
        }
    }

    record Delivery(String recipient, String resetLink, Instant expiresAt) {}
}

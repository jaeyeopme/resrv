package io.resrv.platform.adapter.out.email;

import io.resrv.platform.application.auth.out.PasswordResetEmailPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
class PasswordResetEmailFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    PasswordResetEmailPort unavailablePasswordResetEmailPort() {
        return (recipient, resetLink, expiresAt) -> {
            throw new IllegalStateException("Password reset email delivery is not configured");
        };
    }
}

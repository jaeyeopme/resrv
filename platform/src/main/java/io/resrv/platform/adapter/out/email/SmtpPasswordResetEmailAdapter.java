package io.resrv.platform.adapter.out.email;

import io.resrv.platform.application.auth.out.PasswordResetEmailPort;
import io.resrv.platform.domain.account.AccountEmail;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(JavaMailSender.class)
class SmtpPasswordResetEmailAdapter implements PasswordResetEmailPort {

    private final JavaMailSender mailSender;

    SmtpPasswordResetEmailAdapter(final JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(
            final AccountEmail recipient, final String resetLink, final Instant expiresAt) {
        final var message = new SimpleMailMessage();
        message.setTo(recipient.value());
        message.setSubject("Reset your resrv password");
        message.setText(
                """
                A password reset is required after repeated failed sign-in attempts.

                Reset your password:
                %s

                This link expires at %s.
                """
                        .formatted(resetLink, expiresAt));
        mailSender.send(message);
    }
}

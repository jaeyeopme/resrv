package io.resrv.platform.application.auth;

import io.resrv.platform.application.account.out.AccountQueryPort;
import io.resrv.platform.application.auth.in.LoginCommand;
import io.resrv.platform.application.auth.in.LoginResult;
import io.resrv.platform.application.auth.in.LoginUseCase;
import io.resrv.platform.application.auth.out.PasswordResetEmailPort;
import io.resrv.platform.application.auth.out.SignInAttemptCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionCommandPort;
import io.resrv.platform.application.auth.out.SignInProtectionQueryPort;
import io.resrv.platform.application.auth.out.TokenGenerationPort;
import io.resrv.platform.application.security.out.PasswordHashingPort;
import io.resrv.platform.application.security.out.PasswordResetTokenGeneratorPort;
import io.resrv.platform.application.security.out.PasswordResetTokenHashingPort;
import io.resrv.platform.domain.account.Account;
import io.resrv.platform.domain.account.AccountEmail;
import io.resrv.platform.domain.account.PasswordResetChallenge;
import io.resrv.platform.domain.account.SignInAttemptOutcome;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(
        noRollbackFor = {AuthenticationFailedException.class, PasswordResetRequiredException.class})
public class LoginService implements LoginUseCase {

    private final AccountQueryPort accountQueryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final TokenGenerationPort tokenGenerationPort;
    private final SignInAttemptCommandPort signInAttemptCommandPort;
    private final SignInProtectionCommandPort signInProtectionCommandPort;
    private final SignInProtectionQueryPort signInProtectionQueryPort;
    private final PasswordResetEmailPort passwordResetEmailPort;
    private final PasswordResetTokenGeneratorPort passwordResetTokenGeneratorPort;
    private final PasswordResetTokenHashingPort passwordResetTokenHashingPort;
    private final Clock clock;
    private final String passwordResetPublicBaseUrl;
    private final Duration passwordResetTokenTtl;
    private final String dummyHash;

    public LoginService(
            final AccountQueryPort accountQueryPort,
            final PasswordHashingPort passwordHashingPort,
            final TokenGenerationPort tokenGenerationPort,
            final SignInAttemptCommandPort signInAttemptCommandPort,
            final SignInProtectionCommandPort signInProtectionCommandPort,
            final SignInProtectionQueryPort signInProtectionQueryPort,
            final PasswordResetEmailPort passwordResetEmailPort,
            final PasswordResetTokenGeneratorPort passwordResetTokenGeneratorPort,
            final PasswordResetTokenHashingPort passwordResetTokenHashingPort,
            final Clock clock,
            @Value("${resrv.security.password-reset.public-base-url:http://localhost:8080}")
                    final String passwordResetPublicBaseUrl,
            @Value("${resrv.security.password-reset.token-ttl:PT30M}")
                    final Duration passwordResetTokenTtl) {
        this.accountQueryPort = accountQueryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.tokenGenerationPort = tokenGenerationPort;
        this.signInAttemptCommandPort = signInAttemptCommandPort;
        this.signInProtectionCommandPort = signInProtectionCommandPort;
        this.signInProtectionQueryPort = signInProtectionQueryPort;
        this.passwordResetEmailPort = passwordResetEmailPort;
        this.passwordResetTokenGeneratorPort = passwordResetTokenGeneratorPort;
        this.passwordResetTokenHashingPort = passwordResetTokenHashingPort;
        this.clock = clock;
        this.passwordResetPublicBaseUrl = passwordResetPublicBaseUrl;
        this.passwordResetTokenTtl = passwordResetTokenTtl;
        this.dummyHash = passwordHashingPort.hash("constant-time-dummy");
    }

    @Override
    public LoginResult login(final LoginCommand command) {
        if (command == null || isBlank(command.email()) || isBlank(command.password())) {
            passwordHashingPort.matches(rawPasswordOrEmpty(command), dummyHash);
            throw new AuthenticationFailedException();
        }

        final var email = accountEmail(command.email(), command.password());
        final var account = accountQueryPort.findByEmail(email);
        final var hashedPassword = account.map(Account::hashedPassword).orElse(dummyHash);
        final var passwordMatches = passwordHashingPort.matches(command.password(), hashedPassword);
        if (!passwordMatches) {
            recordFailedAttempt(account, email, command.callerFingerprint());
            throw new AuthenticationFailedException();
        }

        final var activeAccount =
                account.filter(Account::active).orElseThrow(AuthenticationFailedException::new);
        if (signInProtectionQueryPort.requiresPasswordReset(activeAccount.id())) {
            signInAttemptCommandPort.recordAttempt(
                    Optional.of(activeAccount.id()),
                    emailHash(email),
                    callerFingerprint(command),
                    SignInAttemptOutcome.FAILED_REQUIRES_RESET,
                    clock.instant());
            throw new PasswordResetRequiredException();
        }

        signInProtectionCommandPort.clearProtection(activeAccount.id(), clock.instant());
        signInAttemptCommandPort.recordAttempt(
                Optional.of(activeAccount.id()),
                emailHash(email),
                callerFingerprint(command),
                SignInAttemptOutcome.SUCCESS,
                clock.instant());
        return tokenGenerationPort.generate(activeAccount.id());
    }

    private void recordFailedAttempt(
            final Optional<Account> account,
            final AccountEmail email,
            final String callerFingerprint) {
        final var now = clock.instant();
        final var outcome =
                account.isPresent()
                        ? SignInAttemptOutcome.FAILED_BAD_PASSWORD
                        : SignInAttemptOutcome.FAILED_UNKNOWN_ACCOUNT;
        signInAttemptCommandPort.recordAttempt(
                account.map(Account::id),
                emailHash(email),
                callerFingerprint(callerFingerprint),
                outcome,
                now);

        account.ifPresent(
                existingAccount -> {
                    final var protection =
                            signInProtectionCommandPort.recordFailedPasswordAttempt(
                                    existingAccount.id(), now);
                    if (protection.failedPasswordAttempts() == 5
                            && protection.passwordResetRequired()) {
                        sendResetEmail(existingAccount, now);
                    }
                });
    }

    private void sendResetEmail(final Account account, final Instant now) {
        final var token = passwordResetTokenGeneratorPort.generate();
        final var expiresAt = now.plus(passwordResetTokenTtl);
        signInProtectionCommandPort.replaceActivePasswordResetChallenges(account.id(), now);
        signInProtectionCommandPort.createPasswordResetChallenge(
                PasswordResetChallenge.create(
                        account.id(), passwordResetTokenHashingPort.digest(token), now, expiresAt));
        passwordResetEmailPort.sendPasswordResetEmail(
                account.email(), resetLink(token.value()), expiresAt);
    }

    private String resetLink(final String token) {
        return UriComponentsBuilder.fromUriString(passwordResetPublicBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private static String emailHash(final AccountEmail email) {
        return sha256(email.value());
    }

    private static String sha256(final String value) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private AccountEmail accountEmail(final String value, final String rawPassword) {
        try {
            return new AccountEmail(value);
        } catch (final IllegalArgumentException exception) {
            passwordHashingPort.matches(rawPassword, dummyHash);
            throw new AuthenticationFailedException();
        }
    }

    private static String rawPasswordOrEmpty(final LoginCommand command) {
        if (command == null || command.password() == null) {
            return "";
        }
        return command.password();
    }

    private static String callerFingerprint(final LoginCommand command) {
        return command == null ? "unknown" : callerFingerprint(command.callerFingerprint());
    }

    private static String callerFingerprint(final String callerFingerprint) {
        if (callerFingerprint == null || callerFingerprint.isBlank()) {
            return "unknown";
        }
        return callerFingerprint;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

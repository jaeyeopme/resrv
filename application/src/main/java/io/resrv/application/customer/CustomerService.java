package io.resrv.application.customer;

import io.resrv.application.auth.AuthenticationFailedException;
import io.resrv.application.auth.LoginResult;
import io.resrv.application.auth.RoleNames;
import io.resrv.application.auth.out.TokenGenerationPort;
import io.resrv.application.auth.out.UserCredentials;
import io.resrv.application.customer.in.CustomerLoginCommand;
import io.resrv.application.customer.in.CustomerLoginUseCase;
import io.resrv.application.customer.in.CustomerResult;
import io.resrv.application.customer.in.RegisterCustomerCommand;
import io.resrv.application.customer.in.RegisterCustomerUseCase;
import io.resrv.application.customer.out.CustomerCommandPort;
import io.resrv.application.customer.out.CustomerQueryPort;
import io.resrv.application.security.out.PasswordHashingPort;
import io.resrv.application.tenant.TenantNotFoundException;
import io.resrv.application.tenant.out.TenantQueryPort;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.customer.CustomerName;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CustomerService implements RegisterCustomerUseCase, CustomerLoginUseCase {

    private final Clock clock;
    private final TenantQueryPort tenantQueryPort;
    private final CustomerCommandPort customerCommandPort;
    private final CustomerQueryPort customerQueryPort;
    private final PasswordHashingPort passwordHashingPort;
    private final TokenGenerationPort tokenGenerationPort;
    private final String dummyHash;

    CustomerService(
            final Clock clock,
            final TenantQueryPort tenantQueryPort,
            final CustomerCommandPort customerCommandPort,
            final CustomerQueryPort customerQueryPort,
            final PasswordHashingPort passwordHashingPort,
            final TokenGenerationPort tokenGenerationPort) {
        this.clock = clock;
        this.tenantQueryPort = tenantQueryPort;
        this.customerCommandPort = customerCommandPort;
        this.customerQueryPort = customerQueryPort;
        this.passwordHashingPort = passwordHashingPort;
        this.tokenGenerationPort = tokenGenerationPort;
        this.dummyHash = passwordHashingPort.hash("constant-time-customer-dummy");
    }

    @Override
    public CustomerResult register(final RegisterCustomerCommand command) {
        if (isBlank(command.password())) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        final var tenant =
                tenantQueryPort
                        .findBySlug(command.tenantSlug())
                        .orElseThrow(() -> new TenantNotFoundException(command.tenantSlug()));
        final var email = new CustomerEmail(command.email());
        if (customerQueryPort.existsByTenantIdAndEmail(tenant.id(), email)) {
            throw new CustomerEmailAlreadyExistsException(tenant.id(), email);
        }

        final var customer =
                Customer.create(
                        tenant.id(),
                        email,
                        new CustomerName(command.name()),
                        passwordHashingPort.hash(command.password()),
                        clock.instant());
        customerCommandPort.save(customer);
        return CustomerResult.from(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(final CustomerLoginCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new AuthenticationFailedException();
        }
        final var tenantId = tenantQueryPort.findIdBySlug(command.tenantSlug());
        if (tenantId.isEmpty()) {
            passwordHashingPort.matches(command.password(), dummyHash);
            throw new AuthenticationFailedException();
        }

        final var credentials =
                customerQueryPort.findCredentialsByTenantIdAndEmail(
                        tenantId.get(), new CustomerEmail(command.email()).value());
        final var hashedPassword =
                credentials.map(UserCredentials::hashedPassword).orElse(dummyHash);

        if (!passwordHashingPort.matches(command.password(), hashedPassword)) {
            throw new AuthenticationFailedException();
        }

        final var verified =
                credentials
                        .filter(UserCredentials::active)
                        .orElseThrow(AuthenticationFailedException::new);
        final var token =
                tokenGenerationPort.generate(
                        verified.userId(), verified.tenantId(), RoleNames.CUSTOMER);
        return new LoginResult(token.accessToken(), token.expiresIn());
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

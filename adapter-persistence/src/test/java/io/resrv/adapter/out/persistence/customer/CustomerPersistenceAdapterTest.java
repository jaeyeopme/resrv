package io.resrv.adapter.out.persistence.customer;

import static io.resrv.adapter.out.persistence.PersistenceTestFixtures.insertTenantDirectly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.application.auth.RoleNames;
import io.resrv.domain.customer.Customer;
import io.resrv.domain.customer.CustomerEmail;
import io.resrv.domain.customer.CustomerEmailAlreadyExistsException;
import io.resrv.domain.customer.CustomerName;
import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerPersistenceAdapter.class)
class CustomerPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final String HASHED_PASSWORD = "$argon2id$v=19$m=65536,t=3,p=1$test";

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private CustomerPersistenceAdapter adapter;

    @Autowired private CustomerJpaRepository repository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindCredentialsAndDomain() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "customer-save");
        final var customer = createCustomer(tenantId, "Member@Example.com", "Member");

        adapter.save(customer);

        assertTrue(
                adapter.existsByTenantIdAndEmail(
                        tenantId, new CustomerEmail("member@example.com")));
        final var credentials =
                adapter.findCredentialsByTenantIdAndEmail(tenantId, "member@example.com")
                        .orElseThrow();
        assertEquals(customer.id().value(), credentials.userId());
        assertEquals(tenantId.value(), credentials.tenantId());
        assertEquals(HASHED_PASSWORD, credentials.hashedPassword());
        assertEquals(RoleNames.CUSTOMER, credentials.role());
        assertTrue(credentials.active());

        final var found = adapter.findByTenantIdAndId(tenantId, customer.id()).orElseThrow();
        assertEquals("member@example.com", found.email().value());
        assertEquals("Member", found.name().value());
    }

    @Test
    void duplicateEmailInSameTenant_throwsCustomerEmailAlreadyExistsException() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "customer-duplicate");
        adapter.save(createCustomer(tenantId, "member@example.com", "Member One"));

        assertThrows(
                CustomerEmailAlreadyExistsException.class,
                () -> adapter.save(createCustomer(tenantId, "member@example.com", "Member Two")));
    }

    @Test
    void sameEmailInDifferentTenants_isAllowedAndScoped() {
        final var firstTenantId = insertTenantDirectly(jdbcTemplate, NOW, "customer-first");
        final var secondTenantId = insertTenantDirectly(jdbcTemplate, NOW, "customer-second");
        final var firstCustomer = createCustomer(firstTenantId, "member@example.com", "First");
        final var secondCustomer = createCustomer(secondTenantId, "member@example.com", "Second");

        adapter.save(firstCustomer);
        adapter.save(secondCustomer);

        assertEquals(2, repository.count());
        assertTrue(adapter.findByTenantIdAndId(firstTenantId, firstCustomer.id()).isPresent());
        assertFalse(adapter.findByTenantIdAndId(firstTenantId, secondCustomer.id()).isPresent());
    }

    @Test
    void missingCredentials_returnsEmpty() {
        final var tenantId = insertTenantDirectly(jdbcTemplate, NOW, "customer-missing");

        assertFalse(
                adapter.findCredentialsByTenantIdAndEmail(tenantId, "missing@example.com")
                        .isPresent());
    }

    private static Customer createCustomer(
            final TenantId tenantId, final String email, final String name) {
        return Customer.create(
                tenantId, new CustomerEmail(email), new CustomerName(name), HASHED_PASSWORD, NOW);
    }
}

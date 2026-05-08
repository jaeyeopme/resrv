package io.resrv.domain.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.resrv.domain.tenant.TenantId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final TenantId TENANT_ID = TenantId.create();
    private static final CustomerEmail EMAIL = new CustomerEmail("customer@example.com");
    private static final CustomerName NAME = new CustomerName("Jane Customer");
    private static final Instant CREATED_AT = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void create_defaultsToActive() {
        final var customer = Customer.create(TENANT_ID, EMAIL, NAME, "hashed", CREATED_AT);

        assertNotNull(customer.id());
        assertEquals(TENANT_ID, customer.tenantId());
        assertEquals(EMAIL, customer.email());
        assertEquals(NAME, customer.name());
        assertEquals("hashed", customer.hashedPassword());
        assertTrue(customer.active());
        assertEquals(CREATED_AT, customer.createdAt());
    }

    @Test
    void reconstitute_preservesStoredStateAndIdentityEquality() {
        final var id = CustomerId.create();
        final var customer =
                Customer.reconstitute(id, TENANT_ID, EMAIL, NAME, "hashed", false, CREATED_AT);
        final var sameId =
                Customer.reconstitute(
                        id,
                        TENANT_ID,
                        new CustomerEmail("other@example.com"),
                        new CustomerName("Other"),
                        "other-hash",
                        true,
                        CREATED_AT);

        assertEquals(id, customer.id());
        assertEquals(customer, sameId);
        assertEquals(customer.hashCode(), sameId.hashCode());
    }

    @Test
    void rejectsBlankHashedPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Customer.create(TENANT_ID, EMAIL, NAME, " ", CREATED_AT));
    }
}

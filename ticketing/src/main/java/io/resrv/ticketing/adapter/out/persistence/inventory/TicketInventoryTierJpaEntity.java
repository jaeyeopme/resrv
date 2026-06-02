package io.resrv.ticketing.adapter.out.persistence.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_inventory_tier")
class TicketInventoryTierJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_inventory_id", nullable = false)
    private TicketInventoryJpaEntity inventory;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int reserved;

    @Column(nullable = false)
    private int confirmed;

    @Column(name = "soft_reserved", nullable = false)
    private int softReserved;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TicketInventoryTierJpaEntity() {}

    TicketInventoryTierJpaEntity(
            final UUID id,
            final String displayName,
            final int total,
            final int reserved,
            final int confirmed,
            final int softReserved) {
        this.id = id;
        this.displayName = displayName;
        this.total = total;
        this.reserved = reserved;
        this.confirmed = confirmed;
        this.softReserved = softReserved;
    }

    void attachTo(final TicketInventoryJpaEntity inventory, final int sortOrder) {
        this.inventory = inventory;
        this.sortOrder = sortOrder;
    }

    UUID id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    int total() {
        return total;
    }

    int reserved() {
        return reserved;
    }

    int confirmed() {
        return confirmed;
    }

    int softReserved() {
        return softReserved;
    }
}

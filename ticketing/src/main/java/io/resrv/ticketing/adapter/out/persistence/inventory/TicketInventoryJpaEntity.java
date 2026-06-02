package io.resrv.ticketing.adapter.out.persistence.inventory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "ticketing", name = "ticket_inventory")
class TicketInventoryJpaEntity {

    @Id private UUID id;

    @Column(name = "ticket_event_id", nullable = false)
    private UUID ticketEventId;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TicketInventoryTierJpaEntity> tiers = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketInventoryJpaEntity() {}

    TicketInventoryJpaEntity(
            final UUID id,
            final UUID ticketEventId,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.ticketEventId = ticketEventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void replaceTiers(final List<TicketInventoryTierJpaEntity> tiers) {
        this.tiers.clear();
        for (int i = 0; i < tiers.size(); i++) {
            final var tier = tiers.get(i);
            tier.attachTo(this, i);
            this.tiers.add(tier);
        }
    }

    UUID id() {
        return id;
    }

    UUID ticketEventId() {
        return ticketEventId;
    }

    List<TicketInventoryTierJpaEntity> tiers() {
        return List.copyOf(tiers);
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}

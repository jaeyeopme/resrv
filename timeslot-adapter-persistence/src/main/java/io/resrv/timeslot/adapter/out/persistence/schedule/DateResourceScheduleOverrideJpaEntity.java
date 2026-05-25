package io.resrv.timeslot.adapter.out.persistence.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "resource_date_schedule_override")
class DateResourceScheduleOverrideJpaEntity {

    @Id private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "override", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<DateResourceScheduleOverrideWindowJpaEntity> windows = new ArrayList<>();

    protected DateResourceScheduleOverrideJpaEntity() {}

    DateResourceScheduleOverrideJpaEntity(
            final UUID id,
            final UUID businessId,
            final UUID resourceId,
            final LocalDate date,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.businessId = businessId;
        this.resourceId = resourceId;
        this.date = date;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    static DateResourceScheduleOverrideJpaEntity fromDomain(
            final DateResourceScheduleOverride override, final UUID id) {
        final var entity =
                new DateResourceScheduleOverrideJpaEntity(
                        id,
                        override.businessId().value(),
                        override.resourceId().value(),
                        override.date(),
                        override.createdAt(),
                        override.updatedAt());
        for (int i = 0; i < override.windows().size(); i++) {
            entity.addWindow(
                    new DateResourceScheduleOverrideWindowJpaEntity(override.windows().get(i), i));
        }
        return entity;
    }

    DateResourceScheduleOverride toDomain() {
        return DateResourceScheduleOverride.reconstitute(
                BusinessId.of(businessId),
                ResourceId.of(resourceId),
                date,
                windows.stream()
                        .map(DateResourceScheduleOverrideWindowJpaEntity::toDomain)
                        .toList(),
                createdAt,
                updatedAt);
    }

    private void addWindow(final DateResourceScheduleOverrideWindowJpaEntity window) {
        window.attachTo(this);
        windows.add(window);
    }
}

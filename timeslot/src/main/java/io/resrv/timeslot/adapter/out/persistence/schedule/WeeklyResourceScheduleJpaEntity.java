package io.resrv.timeslot.adapter.out.persistence.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "resource_weekly_schedule")
class WeeklyResourceScheduleJpaEntity {

    @Id private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<WeeklyResourceScheduleWindowJpaEntity> windows = new ArrayList<>();

    protected WeeklyResourceScheduleJpaEntity() {}

    WeeklyResourceScheduleJpaEntity(
            final UUID id,
            final UUID businessId,
            final UUID resourceId,
            final int dayOfWeek,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.businessId = businessId;
        this.resourceId = resourceId;
        this.dayOfWeek = dayOfWeek;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    static WeeklyResourceScheduleJpaEntity fromDomain(
            final WeeklyResourceSchedule schedule, final UUID id) {
        final var entity =
                new WeeklyResourceScheduleJpaEntity(
                        id,
                        schedule.businessId().value(),
                        schedule.resourceId().value(),
                        schedule.dayOfWeek().getValue(),
                        schedule.createdAt(),
                        schedule.updatedAt());
        for (int i = 0; i < schedule.windows().size(); i++) {
            entity.addWindow(
                    new WeeklyResourceScheduleWindowJpaEntity(schedule.windows().get(i), i));
        }
        return entity;
    }

    WeeklyResourceSchedule toDomain() {
        return WeeklyResourceSchedule.reconstitute(
                BusinessId.of(businessId),
                ResourceId.of(resourceId),
                DayOfWeek.of(dayOfWeek),
                windows.stream().map(WeeklyResourceScheduleWindowJpaEntity::toDomain).toList(),
                createdAt,
                updatedAt);
    }

    private void addWindow(final WeeklyResourceScheduleWindowJpaEntity window) {
        window.attachTo(this);
        windows.add(window);
    }
}

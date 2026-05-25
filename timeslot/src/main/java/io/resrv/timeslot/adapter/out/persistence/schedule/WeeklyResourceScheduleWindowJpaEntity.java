package io.resrv.timeslot.adapter.out.persistence.schedule;

import io.resrv.timeslot.domain.schedule.ScheduleWindow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(schema = "timeslot", name = "resource_weekly_schedule_window")
class WeeklyResourceScheduleWindowJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WeeklyResourceScheduleJpaEntity schedule;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected WeeklyResourceScheduleWindowJpaEntity() {}

    WeeklyResourceScheduleWindowJpaEntity(final ScheduleWindow window, final int sortOrder) {
        this.id = UUID.randomUUID();
        this.startTime = window.startTime();
        this.endTime = window.endTime();
        this.sortOrder = sortOrder;
    }

    void attachTo(final WeeklyResourceScheduleJpaEntity schedule) {
        this.schedule = schedule;
    }

    ScheduleWindow toDomain() {
        return new ScheduleWindow(startTime, endTime);
    }
}

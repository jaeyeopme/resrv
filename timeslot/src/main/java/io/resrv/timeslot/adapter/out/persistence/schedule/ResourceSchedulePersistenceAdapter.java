package io.resrv.timeslot.adapter.out.persistence.schedule;

import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleCommandPort;
import io.resrv.timeslot.application.schedule.out.ResourceScheduleQueryPort;
import io.resrv.timeslot.domain.schedule.DateResourceScheduleOverride;
import io.resrv.timeslot.domain.schedule.WeeklyResourceSchedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class ResourceSchedulePersistenceAdapter
        implements ResourceScheduleCommandPort, ResourceScheduleQueryPort {

    private final WeeklyResourceScheduleJpaRepository weeklyRepository;
    private final DateResourceScheduleOverrideJpaRepository dateOverrideRepository;

    ResourceSchedulePersistenceAdapter(
            final WeeklyResourceScheduleJpaRepository weeklyRepository,
            final DateResourceScheduleOverrideJpaRepository dateOverrideRepository) {
        this.weeklyRepository = weeklyRepository;
        this.dateOverrideRepository = dateOverrideRepository;
    }

    @Override
    public void saveWeekly(final WeeklyResourceSchedule schedule) {
        weeklyRepository
                .findByBusinessIdAndResourceIdAndDayOfWeek(
                        schedule.businessId().value(),
                        schedule.resourceId().value(),
                        schedule.dayOfWeek().getValue())
                .ifPresent(
                        existing -> {
                            weeklyRepository.delete(existing);
                            weeklyRepository.flush();
                        });
        weeklyRepository.save(
                WeeklyResourceScheduleJpaEntity.fromDomain(schedule, UUID.randomUUID()));
    }

    @Override
    public void saveDateOverride(final DateResourceScheduleOverride override) {
        dateOverrideRepository
                .findByBusinessIdAndResourceIdAndDate(
                        override.businessId().value(),
                        override.resourceId().value(),
                        override.date())
                .ifPresent(
                        existing -> {
                            dateOverrideRepository.delete(existing);
                            dateOverrideRepository.flush();
                        });
        dateOverrideRepository.save(
                DateResourceScheduleOverrideJpaEntity.fromDomain(override, UUID.randomUUID()));
    }

    @Override
    @Transactional
    public void deleteDateOverride(
            final BusinessId businessId, final ResourceId resourceId, final LocalDate date) {
        dateOverrideRepository.deleteByBusinessIdAndResourceIdAndDate(
                businessId.value(), resourceId.value(), date);
    }

    @Override
    public Optional<WeeklyResourceSchedule> findWeekly(
            final BusinessId businessId, final ResourceId resourceId, final DayOfWeek dayOfWeek) {
        return weeklyRepository
                .findByBusinessIdAndResourceIdAndDayOfWeek(
                        businessId.value(), resourceId.value(), dayOfWeek.getValue())
                .map(WeeklyResourceScheduleJpaEntity::toDomain);
    }

    @Override
    public Optional<DateResourceScheduleOverride> findDateOverride(
            final BusinessId businessId, final ResourceId resourceId, final LocalDate date) {
        return dateOverrideRepository
                .findByBusinessIdAndResourceIdAndDate(businessId.value(), resourceId.value(), date)
                .map(DateResourceScheduleOverrideJpaEntity::toDomain);
    }
}

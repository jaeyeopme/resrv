package io.resrv.timeslot.adapter.out.persistence.schedule;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WeeklyResourceScheduleJpaRepository
        extends JpaRepository<WeeklyResourceScheduleJpaEntity, UUID> {

    Optional<WeeklyResourceScheduleJpaEntity> findByBusinessIdAndResourceIdAndDayOfWeek(
            UUID businessId, UUID resourceId, int dayOfWeek);
}

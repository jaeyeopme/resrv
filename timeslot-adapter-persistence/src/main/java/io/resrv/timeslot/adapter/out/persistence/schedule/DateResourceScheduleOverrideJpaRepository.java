package io.resrv.timeslot.adapter.out.persistence.schedule;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DateResourceScheduleOverrideJpaRepository
        extends JpaRepository<DateResourceScheduleOverrideJpaEntity, UUID> {

    Optional<DateResourceScheduleOverrideJpaEntity> findByBusinessIdAndResourceIdAndDate(
            UUID businessId, UUID resourceId, LocalDate date);

    void deleteByBusinessIdAndResourceIdAndDate(UUID businessId, UUID resourceId, LocalDate date);
}

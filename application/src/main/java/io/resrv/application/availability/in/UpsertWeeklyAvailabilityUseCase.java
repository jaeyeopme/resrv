package io.resrv.application.availability.in;

public interface UpsertWeeklyAvailabilityUseCase {

    WeeklyAvailabilityResult upsert(UpsertWeeklyAvailabilityCommand command);
}

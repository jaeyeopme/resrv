package io.resrv.application.availability.in;

public interface UpsertDateAvailabilityOverrideUseCase {

    DateAvailabilityOverrideResult upsert(UpsertDateAvailabilityOverrideCommand command);
}

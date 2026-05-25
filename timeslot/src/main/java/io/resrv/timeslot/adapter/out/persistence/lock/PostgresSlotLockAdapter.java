package io.resrv.timeslot.adapter.out.persistence.lock;

import io.resrv.shared.kernel.ResourceId;
import io.resrv.timeslot.application.lock.out.SlotLockPort;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
class PostgresSlotLockAdapter implements SlotLockPort {

    private final EntityManager entityManager;

    PostgresSlotLockAdapter(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockSlot(final ResourceId resourceId, final Instant slotStartAt) {
        entityManager
                .createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", resourceId.value() + "|" + slotStartAt)
                .getSingleResult();
    }
}

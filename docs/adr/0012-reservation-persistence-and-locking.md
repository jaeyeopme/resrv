# ADR-0012: Reservation Persistence And Locking

## Status

Accepted.

## Date

2026-05-25

## History

- `a4a0ec4 feat(reservation): persist fact based reservations`
- `1b0f181 refactor(timeslot): centralize slot policy`

## Context

Hold correctness needs concurrency control. Active blockers are unexpired holds, confirmed
reservations, and checked-in reservations. Expired holds should stop blocking without mutation.

## Decision

Persist reservation facts in `timeslot.reservation`.

Use:

- `SlotLockPort` for hold-path locking.
- PostgreSQL advisory transaction lock keyed by resource ID and slot start instant.
- Active blocker query for overlapping resource/time ranges after slot-id revalidation.
- Pessimistic row lock for mutation of an existing reservation.
- Domain-level blocker semantics through `Reservation.blocksSlotAt(now)`.

Generated virtual slots for the same resource are non-overlapping under the effective schedule and
policy. The overlap query remains a defensive backend check for stale, invalid, or policy-drifted
slot identities.

## Alternatives

### Database exclusion constraint on active status

The old tenant API used persisted status-based overlap prevention. The new model derives active
hold state from `hold_expires_at > now`, which cannot be expressed as a stable partial index
predicate for correctness.

### Rely only on application query without lock

Concurrent hold attempts could both observe no blocker and insert conflicting holds.

## Consequences

- Multi-node instances share correctness through PostgreSQL.
- Cleanup jobs are not part of capacity correctness.
- Lock key stability is critical.
- Active blocker query must stay aligned with derived reservation state and
  `Reservation.blocksSlotAt(now)`.
- Blocked hold creation, expired-hold confirmation, and conflicting lifecycle transitions surface as
  public `409 Conflict` responses.

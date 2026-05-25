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
- Active blocker query for overlapping resource/time ranges.
- Pessimistic row lock for mutation of an existing reservation.
- Domain-level blocker semantics through `Reservation.blocksSlotAt(now)`.

## Alternatives

### Database Exclusion Constraint On Active Status

The old tenant API used persisted status-based overlap prevention. The new model derives active
hold state from `hold_expires_at > now`, which cannot be expressed as a stable partial index
predicate for correctness.

### Rely Only On Application Query Without Lock

Concurrent hold attempts could both observe no blocker and insert conflicting holds.

## Consequences

- Multi-node instances share correctness through PostgreSQL.
- Cleanup jobs are not part of capacity correctness.
- Lock key stability is critical.
- Active blocker query must stay aligned with derived reservation state and
  `Reservation.blocksSlotAt(now)`.

# ADR-0011: Derived Reservation State

## Status

Accepted.

## Date

2026-05-25

## History

- `f762ae3 feat(reservation): derive state from facts`

## Context

Reservations have states that depend on user actions and time. `HELD` and `EXPIRED` are especially
time-sensitive: a hold expires when time passes, even if no worker updates the row.

## Decision

Persist timestamp facts and derive state at read/use time.

Facts:

- `hold_expires_at`
- `confirmed_at`
- `released_at`
- `cancelled_at`
- `cancelled_by`
- `checked_in_at`
- `no_show_at`

Do not persist `HELD` or `EXPIRED` as status values.

## Alternatives

### Persist Reservation Status Enum

This is easy to query but makes time-derived state stale without scheduled mutation.

### Event Store Reservation Transitions

An event store preserves richer history but is unnecessary for current scope.

## Consequences

- Time passing can change reservation state without database writes.
- Domain invariants must reject conflicting terminal facts.
- Queries must encode active-state logic explicitly.


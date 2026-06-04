# ADR-0002: Shared Kernel Identity Primitives

## Status

Accepted.

## Date

2026-05-25

## History

- `ba29e58 feat: add shared identity primitives`

## Context

Platform and timeslot both need stable identifiers for accounts, businesses, resources, and
reservations. They also share timezone handling for business-local schedule generation.

Duplicating these primitives per bounded context would create conversion noise and inconsistent
validation.

## Decision

Create a `shared-kernel` module containing:

- `AccountId`
- `BusinessId`
- `ResourceId`
- `ReservationId`
- `Timezone`

The shared kernel stays intentionally small and only contains primitives that are stable across
bounded contexts.

## Alternatives

### Duplicate IDs per context

This keeps contexts purer, but it would add boilerplate mapping between platform and timeslot for
the same UUID values.

### Put IDs in a general common module

A broad common module tends to collect unrelated utilities. `shared-kernel` is narrower and should
not become a dumping ground.

## Consequences

- Platform and timeslot can share ID types without sharing domain models.
- `shared-kernel` must remain stable and small.
- New shared types require high scrutiny because they create cross-context coupling.

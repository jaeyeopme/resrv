# Architecture

`resrv` is organized around bounded contexts and hexagonal boundaries.

## Bounded Contexts

| Context | Owns |
|---|---|
| Platform | Account identity, login, business creation, business membership |
| Timeslot | Booking settings, resources, schedules, virtual slots, reservations |
| Shared kernel | Stable identity and time primitives shared by contexts |

## Current Module State

The current branch uses bounded-context Gradle modules. This is recorded in
[ADR-0017](adr/0017-collapse-to-bounded-context-modules.md).

```text
shared-kernel
platform
timeslot
```

[ADR-0001](adr/0001-bounded-context-module-baseline.md) records the superseded 11-module baseline.
Hexagonal layers are enforced as packages.

## Dependency Direction

Dependency direction points inward:

```text
api/runtime -> adapters -> application -> domain
```

Rules:

- Domain code must not depend on Spring, JPA, adapters, application services, or API runtime.
- Application code defines ports and use cases.
- Adapters implement ports.
- API packages assemble web, persistence, security, and configuration.
- Timeslot code must not depend on platform domain/application code directly.

## Platform Context

Platform uses:

- `Account` for identity.
- `Business` for organization ownership.
- `BusinessMembership` for `OWNER` and `STAFF` access.

Account-scoped JWTs identify the caller. Business access is resolved server-side from membership
data.

## Timeslot Context

Timeslot uses:

- `BusinessBookingSettings` for defaults.
- `Resource` for bookable capacity.
- Weekly schedules and date override schedules.
- Virtual slots encoded as opaque `slotId`.
- Reservation facts for lifecycle transitions.

Slots are not persisted. They are generated from business timezone, settings, resource overrides,
and schedule windows.

## Reservation Correctness

Hold creation uses PostgreSQL advisory transaction locking plus an active blocker query. This keeps
correctness independent from cleanup jobs.

Active blockers:

- Confirmed reservations.
- Holds whose `holdExpiresAt` is still in the future.

Released, cancelled, no-show, and expired holds do not block capacity.

## Decision Log

Architecture decisions live in [docs/adr](adr/).

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
- Timeslot code must not depend on platform domain, adapters, API runtime, repositories, entities,
  or persistence schema. Its outbound platform adapter may depend only on explicit platform
  `platform.contract` types; platform-owned contract configuration wires the service and persistence
  implementation.
- Direct database access primitives are limited to outbound adapters.

## Persistence Access Policy

Owned persistence defaults to Spring Data JPA repositories inside `adapter.out.persistence`.
Database-specific behavior may use native SQL only inside outbound adapters. Current production use
is PostgreSQL advisory locking in timeslot persistence.

## Platform Context

Platform uses:

- `Account` for identity.
- `Business` for organization ownership.
- `BusinessMembership` for `OWNER` and `STAFF` access.
- Sign-in protection and password reset challenges for account recovery.

Account-scoped JWTs identify the caller. Business access is resolved server-side from membership
data.

## Account Security

Platform owns repeated password failure tracking, password reset challenge persistence, reset token
digesting, password hash update, and SMTP-compatible reset email delivery. Password reset delivery is
an outbound adapter behind an application port.

Active-state checks stay server-side. Platform protected requests reject inactive accounts after JWT
authentication. Business-scoped owner/staff decisions require active account, active business, and
active membership. Timeslot consumes those decisions only through `platform.contract` types.

## Timeslot Context

Timeslot uses:

- `BusinessBookingSettings` for defaults.
- `Resource` for bookable capacity.
- `EffectiveBookingPolicy` to resolve business defaults and resource overrides.
- Weekly schedules and date override schedules.
- Virtual slots encoded as opaque `slotId`.
- Reservation facts for lifecycle transitions.

Slots are not persisted. `SlotGenerator` creates them from business timezone, effective booking
policy, and schedule windows.

Public booking discovery remains reachable for anonymous callers, but inactive businesses or
resources produce no bookable resource or slot results.

## Reservation Correctness

Hold creation uses PostgreSQL advisory transaction locking plus an active blocker query. This keeps
correctness independent from cleanup jobs.

Active blockers:

- Confirmed reservations.
- Checked-in reservations.
- Holds whose `holdExpiresAt` is still in the future.

Released, cancelled, no-show, and expired holds do not block capacity.

## Decision Log

Architecture decisions live in [docs/adr](adr/).

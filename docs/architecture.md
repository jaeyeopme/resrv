# Architecture

`resrv` is organized around bounded contexts and hexagonal boundaries.

## Bounded Contexts

| Context | Owns |
|---|---|
| Platform | Account identity, login, business creation, business membership |
| Platform exchange | Published platform-owned lookup and decision APIs for other contexts |
| Timeslot | Booking settings, resources, schedules, virtual slots, reservations |
| Ticketing | Ticket sale events, sale windows, tiered inventory, selected seats, and ticket purchases |
| Shared kernel | Stable identity and time primitives shared by contexts |

## Current Module State

The backend uses bounded-context Gradle modules. The bounded-context collapse is
recorded in [ADR-0017](adr/0017-collapse-to-bounded-context-modules.md),
and the dedicated exchange module is recorded in
[ADR-0020](adr/0020-platform-exchange-boundary.md).

```text
shared-kernel
platform-exchange
platform
ticketing
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
- Timeslot and ticketing code must not depend on platform domain, adapters, API runtime,
  repositories, entities, or persistence schema. Their outbound platform adapters may depend only on
  explicit `platform-exchange` APIs. Platform application services implement those APIs inside the
  platform module.
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
- `BusinessMembershipAuditEntry` for append-only grant, reactivation, role-change, and disablement
  history.
- Sign-in protection and password reset challenges for account recovery.

Account-scoped JWTs identify the caller. Business access is resolved server-side from membership
data.

## Account Security

Platform owns repeated password failure tracking, password reset challenge persistence, reset token
digesting, password hash update, and SMTP-compatible reset email delivery. Password reset delivery is
an outbound adapter behind an application port.

Active-state checks stay server-side. Platform protected requests reject inactive accounts after JWT
authentication. Business-scoped owner/staff decisions require active account, active business, and
active membership. Timeslot consumes those decisions only through `platform-exchange` APIs.
Membership administration operations are owner-only and preserve at least one active owner per
business.

Platform exposes separate cross-context contracts for different intents:

- `ActiveBusinessLookup`: returns only active businesses for availability, settings, scheduling, and
  booking flows.
- `BusinessSummaryLookup`: returns current display summary data and may include inactive businesses
  for historical customer-owned reservation rendering.
- `BusinessAccessCheck`: returns an authorization decision for business-scoped owner/staff actions;
  callers must not treat a false result as evidence about which underlying record is missing or
  inactive.

See [ADR-0019](adr/0019-platform-contracts-for-timeslot-reads.md) for the decision to use
synchronous platform exchange APIs in the current modular monolith and keep event-backed summary
projections as a future option. See [ADR-0020](adr/0020-platform-exchange-boundary.md) for the
module boundary that keeps those APIs out of the platform implementation module.

## Timeslot Context

Timeslot uses:

- `BusinessBookingSettings` for defaults.
- `Resource` for bookable capacity, identified externally and internally by stable resource ID.
- `EffectiveBookingPolicy` to resolve business defaults and resource overrides.
- Weekly schedules and date override schedules.
- Virtual slots encoded as opaque `slotId`.
- Reservation facts for lifecycle transitions.

Slots are not persisted. `SlotGenerator` creates them from business timezone, effective booking
policy, and schedule windows.

Generated slots for one resource do not overlap under the effective policy. Hold requests still
decode and revalidate the opaque `slotId` against current business, resource, timezone, schedule,
policy, and current time before persistence.

Public booking discovery remains reachable for anonymous callers, but inactive businesses or
resources produce no bookable resource or slot results.

Resource names are display/search metadata and are not unique within a business. Timeslot does not
expose resource slugs, handles, or URL-safe resource keys. Business slug remains platform-owned and
continues to scope public booking discovery; resource-scoped public discovery uses business slug
plus resource ID.

## Ticketing Context

Ticketing owns ticket sale and purchase lifecycle data:

- `TicketEvent` for a sale opportunity owned by a platform business id.
- `TicketEventProfile` for display title and event occurrence timing.
- `TicketSaleWindow` for sale start/end boundaries.
- `TicketInventory` and `TicketInventoryTier` for tiered capacity counters.
- `TicketSeat` for event-owned selected seats and purchase status.
- `TicketPurchase` for durable customer ownership of purchased seats.

Ticketing stores platform `businessId` references locally and resolves platform business facts only
through `platform-exchange`. It does not add cross-schema foreign keys to platform tables and does
not read platform persistence directly.

Ticketing exposes selected-seat purchase confirmation, customer ticket history, and authorized
business purchase activity through the platform runtime. Purchase confirmation is the first durable
ticket lifecycle action: it creates no pre-purchase checkout attempt, hold, cancellation,
expiration, or failed-attempt record. Same-customer retries for the same purchased seats return the
existing purchase; later attempts by other customers fail as unavailable.

Business purchase activity access is resolved server-side through `platform-exchange` membership
checks. Missing events and events outside the caller's business authority return the same public
not-found style response.

Ticketing IDs are the external identity strategy for ticketing APIs. Slugs, handles, event keys,
title uniqueness, and separate public opaque identifiers are intentionally excluded from this
baseline. Real payment authorization/settlement, queueing, waitlists, resale, and recurring or
multi-session events remain future scope.

## Reservation Correctness

Hold creation uses PostgreSQL advisory transaction locking plus an active blocker query. This keeps
correctness independent from cleanup jobs.

Active blockers:

- Confirmed reservations.
- Checked-in reservations.
- Holds whose `holdExpiresAt` is still in the future.

Released, cancelled, no-show, and expired holds do not block capacity.

Reservation lifecycle mutations load the reservation row with a pessimistic write lock before
confirm, release, cancel, check-in, or no-show facts are written. Public API behavior treats blocked
holds, expired-hold confirmation, and conflicting lifecycle transitions as `409 Conflict` while
keeping IDOR-sensitive customer reservation probes as not-found style responses.

Reservation error responses distinguish invalid request shape, unavailable slot identity, and
runtime state conflicts:

- `400 Bad Request`: malformed JSON, missing required fields, invalid UUIDs, or invalid enum values.
- `409 Conflict`: a valid request conflicts with current capacity or lifecycle state, such as an
  already blocked slot, expired hold confirmation, or a conflicting reservation transition.
- `422 Unprocessable Entity`: a syntactically valid hold request references a slot identity that is
  stale, policy-drifted, outside booking range, unavailable, or otherwise not currently bookable.

## Decision Log

Architecture decisions live in [docs/adr](adr/).

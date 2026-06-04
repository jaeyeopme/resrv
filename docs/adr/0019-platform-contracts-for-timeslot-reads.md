# ADR-0019: Platform Contracts For Timeslot Reads

## Status

Accepted.

## Date

2026-05-28

## Context

`timeslot` owns booking settings, resources, schedules, slots, and reservations. `platform` owns
accounts, businesses, and business memberships.

Customer reservation history needs to render current business display data for reservations that
belong to the authenticated customer. Some of those reservations may refer to businesses or resources
that are now inactive. Business booking and staff operations, however, still need active-business
availability checks and owner/staff authorization decisions.

In a fully asynchronous context integration, `platform` could publish business lifecycle events and
`timeslot` could maintain its own read model for business display summaries. That would reduce
synchronous read coupling, but it would add event schema versioning, replay/backfill mechanics,
projection repair, and eventual consistency. Authorization decisions also need current active
account, active business, and active membership facts, so they are poor candidates for stale
projection-only enforcement.

This codebase is currently a modular monolith. Context boundaries are package-enforced inside
bounded-context Gradle modules, and cross-context calls are allowed only through explicit
platform exchange APIs.

## Decision

Use explicit synchronous platform exchange APIs for timeslot-to-platform communication:

- `ActiveBusinessLookup` returns only active businesses for booking availability, settings,
  scheduling, and other flows where inactive businesses must be unavailable.
- `BusinessSummaryLookup.findCurrentSummaryById(...)` returns current business display summary data
  and may include inactive businesses for historical customer-owned reservation rendering.
- `BusinessAccessCheck` returns an authorization decision for business-scoped owner/staff actions.
  Consumers must not infer whether the account, business, or membership exists from a false result.

Timeslot adapts these contracts through outbound ports. Timeslot must not read platform tables
directly and must not depend on platform domain, adapters, repositories, API runtime, or persistence
schema.

## Alternatives

### Platform event projection in timeslot

Platform would publish business lifecycle events and timeslot would store a local business summary
projection.

Benefits:

- Timeslot customer history reads would not synchronously call platform.
- Current display summary reads could continue during transient platform lookup failures.

Costs:

- Requires event schema versioning, replay/backfill, projection repair, and operational monitoring.
- Introduces eventual consistency into customer-visible display data.
- Does not remove the need for fresh synchronous authorization decisions.

This remains a future option if business summary reads become high-volume or platform/timeslot move
out of a modular monolith.

### Single generic business lookup

Expose one `findById` style business lookup and let callers decide how to use it.

Benefits:

- Fewer types and methods.

Costs:

- Hides whether inactive businesses are included.
- Makes it easier to accidentally use display lookup for authorization or availability.
- Weakens bounded-context intent in call sites.

Rejected in favor of intent-specific contract names.

## Consequences

- Contract names must encode intent, not just data shape.
- Customer reservation history can render inactive business summaries without weakening booking
  availability or owner/staff authorization.
- Authorization remains a decision contract, not a lookup contract.
- Future event-driven projections can replace `BusinessSummaryLookup` without changing the
  authorization path.

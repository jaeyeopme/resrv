# ADR-0014: Timeslot Booking API Boundary

## Status

Accepted.

## Date

2026-05-25

## History

- `195b6b5 feat(api): expose timeslot booking API`

## Context

After timeslot application and persistence behavior existed, the booking workflow needed a REST API
surface. Timeslot still must not depend on platform domain or platform application code.

## Decision

Expose timeslot endpoints for:

- Booking settings.
- Resource create/list.
- Weekly schedule replacement.
- Date override replacement.
- Slot listing.
- Reservation hold, confirm, release, cancel, check-in, and no-show.
- Business reservation list/search by business-local date with optional resource, customer account,
  and state filters.

Add a `PlatformBusinessLookupAdapter` outbound adapter that implements timeslot ports by calling
explicit `platform.contract` lookup/access types. Timeslot does not read platform tables or columns
directly.

Timeslot security accepts account-scoped JWTs and resolves business access server-side.

## Alternatives

### Put Timeslot Controllers In Platform API

This would reduce runtime modules but erase the boundary between platform lifecycle and booking
workflow.

### Let Timeslot Read Platform Schema Directly

This avoids a compile-time platform dependency but couples timeslot to platform table and column
names.

## Consequences

- Timeslot owns its HTTP surface.
- Cross-context reads go through narrow ports.
- Timeslot may depend only on explicit `platform.contract` types, not platform application
  services, domain, repositories, entities, or persistence schema.
- Timeslot runtime packaging remains pending because `bootJar` and `bootRun` are currently
  disabled.

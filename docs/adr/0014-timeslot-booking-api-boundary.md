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

Add a `PlatformBusinessLookupAdapter` in the timeslot API runtime that implements timeslot ports by
reading platform persistence data.

Timeslot security accepts account-scoped JWTs and resolves business access server-side.

## Alternatives

### Put Timeslot Controllers In Platform API

This would reduce runtime modules but erase the boundary between platform lifecycle and booking
workflow.

### Let Timeslot Depend On Platform Application Services

This would reuse code directly but couples bounded contexts at the application layer.

## Consequences

- Timeslot owns its HTTP surface.
- Cross-context reads go through narrow ports.
- Timeslot runtime packaging remains pending because `bootJar` and `bootRun` are currently
  disabled.

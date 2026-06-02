# ADR-0023: Ticketing Bounded Context

## Status

Accepted

## Context

Ticket sales need event, sale-window, and inventory primitives before public claim, hold, payment,
queue, or waitlist behavior can be implemented. These concepts are related to reservations but are
not timeslot resources, schedules, virtual slots, or reservation lifecycle transitions.

The current backend supports one canonical Spring Boot runtime from the `platform` module. Existing
cross-context platform facts are exposed through the pure Java `platform-exchange` module.

## Decision

Add a `ticketing` Gradle module as its own bounded context. It owns ticket events, event occurrence
metadata, sale windows, ticket inventory, and tier capacity counters.

The `ticketing` module is assembled into the existing `platform` runtime. Its `bootJar` and
`bootRun` tasks remain disabled so it does not become a second supported backend runtime.

Ticketing stores platform `businessId` references locally and uses `platform-exchange` for
platform-owned business lookup and access decisions. Ticketing must not read platform tables
directly and must not add cross-schema foreign keys to platform persistence.

Ticketing-owned IDs are the identity strategy for ticket events, inventories, and tiers. Slugs,
handles, event keys, title uniqueness, and separate public opaque identifiers are intentionally
excluded from the baseline.

## Consequences

- Future ticketing APIs can expose ticketing-owned IDs directly; authorization must come from
  server-side checks, not identifier secrecy.
- Public ticketing endpoints require a separate feature and generated OpenAPI coverage.
- Claim, hold, payment, queue, waitlist, multi-session, and recurring event behavior remain future
  scope.
- Ticketing can evolve without coupling ticket sale logic into the timeslot reservation model.

# ADR-0015: Replace Tenant Booking API

## Status

Accepted.

## Date

2026-05-25

## History

- `1657ea5 refactor: replace tenant booking API`

## Context

The branch first built the platform/timeslot redesign alongside the old tenant-local API. Keeping
both indefinitely would leave two competing sources of truth for accounts, businesses, resources,
schedules, and reservations.

## Decision

Remove the old tenant-local modules and API:

- `domain`
- `application`
- `adapter-persistence`
- `adapter-web`
- `bootstrap`

Replace them with the platform/timeslot module set.

## Alternatives

### Keep Both APIs Temporarily

This eases migration but doubles documentation, tests, and maintenance in a project that does not
need backward compatibility yet.

### Migrate Incrementally Per Endpoint

This is safer for production systems. This repository is still pre-production, so a branch-level
replacement is acceptable.

## Consequences

- Old tenant/admin/customer terminology should not remain in source-of-truth docs.
- Old documentation assets and generated artifacts may become stale.
- Merge review must focus on the redesign as a replacement, not a compatible extension.

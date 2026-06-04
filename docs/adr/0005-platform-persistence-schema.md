# ADR-0005: Platform Persistence Schema

## Status

Accepted.

## Date

2026-05-25

## History

- `5686274 feat(platform): persist accounts and businesses`
- `930e452 fix(platform): harden persistence constraints`

## Context

Platform identity and business ownership need durable storage with database-level guarantees.
Domain validation is not enough because concurrent writes and adapter bugs can still reach the
database.

## Decision

Create `platform` schema with:

- `platform.account`
- `platform.business`
- `platform.business_membership`

Add database constraints for:

- Unique account email.
- Unique business slug.
- Unique account/business membership pair.
- Non-blank required fields.
- Valid status and role values.
- Foreign keys from membership to account and business.

Translate persistence constraint violations into application-level duplicate exceptions where
needed.

## Alternatives

### Application-only uniqueness checks

These are useful for clear errors but do not protect against races.

### Single public schema

A single schema is simpler, but schema separation makes platform/timeslot ownership visible in the
database.

## Consequences

- Platform tables are owned by platform persistence adapters.
- Database constraints become part of correctness, not just documentation.
- Timeslot does not add cross-schema foreign keys to platform in the current design.

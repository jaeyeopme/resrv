# ADR-0007: Timeslot Booking Settings

## Status

Accepted.

## Date

2026-05-25

## History

- `bbcfef5 feat(timeslot): add booking settings`
- `d91cdf0 fix(timeslot): validate settings before lookup`

## Context

Timeslot booking needs business-level defaults for slot generation and reservation policy.
Settings should not exist for unavailable businesses, and invalid values should fail before
database lookup or persistence.

## Decision

Create `BusinessBookingSettings` with:

- Slot duration.
- Hold TTL.
- Cancellation window.
- Max advance booking days.

Before upsert, validate command values and confirm the business is active through a
`BusinessLookupPort`.

## Alternatives

### Hard-code booking policy

This is simpler, but each business needs configurable booking behavior.

### Store settings in platform

Booking policy belongs to timeslot, not platform identity and membership.

## Consequences

- Timeslot application depends on a narrow business lookup port, not platform domain code.
- Settings validation happens before persistence.
- Resource-level overrides can later fall back to business settings.

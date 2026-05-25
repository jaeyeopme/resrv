# ADR-0013: Reservation Transition Authorization

## Status

Accepted.

## Date

2026-05-25

## History

- `18e9a72 feat(reservation): add hold transition flows`

## Context

Reservation transitions have different actors:

- Customer account that owns the reservation.
- Business owner/staff acting on behalf of the business.

Authorization must be checked in application use cases, not only web controllers.

## Decision

Implement reservation use cases for:

- Hold.
- Confirm.
- Release.
- Customer cancel.
- Business cancel.
- Check-in.
- No-show.

Rules:

- Confirm, release, and customer cancel require reservation ownership.
- Business cancel, check-in, and no-show require active business access through
  `BusinessAccessPort`.
- Customer cancel must respect cancellation cutoff.
- Check-in cannot happen before start time.
- No-show cannot happen before end time.

## Alternatives

### Controller-Only Authorization

This is easier initially but lets non-web adapters bypass rules.

### Role Claims In JWT

This avoids a membership lookup but duplicates business access state into tokens.

## Consequences

- Authorization is part of application behavior.
- Web adapters remain thin.
- Tests can verify transition rules without HTTP.


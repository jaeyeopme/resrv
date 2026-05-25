# ADR-0003: Platform Account Identity

## Status

Accepted.

## Date

2026-05-25

## History

- `416db14 feat(auth): add platform accounts`
- `f7712df fix(auth): validate account credentials`

## Context

The original API had tenant-local administrator and customer identities. The redesign needs one
platform identity that can act as owner, staff, or customer depending on business membership and
reservation ownership.

## Decision

Use `Account` as the platform identity model.

`Account` includes:

- Email.
- Name.
- Hashed password.
- Status.
- Creation timestamp.

Registration validates email, name, and password before persistence. Account credentials are
validated at command and domain boundaries.

## Alternatives

### Keep Separate Admin And Customer Identities

This preserves old terminology but duplicates identity, login, and token flows.

### Model Customer As A Separate Aggregate Immediately

This may be needed later for profile, preferences, or CRM data. It is not required for reservation
ownership in the current scope.

## Consequences

- Owners, staff, and customers all authenticate as accounts.
- Role and business access are not account properties.
- Customer reservation ownership can reference `customer_account_id`.


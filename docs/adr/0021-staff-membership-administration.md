# ADR-0021: Staff Membership Administration

## Status

Accepted.

## Date

2026-05-29

## Context

Platform already owns `Account`, `Business`, and current `BusinessMembership` state. Owner-created
businesses receive an initial active `OWNER` membership, and request-time business authorization
uses active membership state rather than JWT role claims.

Owners also need to grant staff access, review current access, change roles, disable access, and
inspect access-change history. Authorization checks need a cheap current-state read path, while
access review needs historical entries.

## Decision

Keep `platform.business_membership` as the current-state membership row and add append-only
`platform.business_membership_audit_entry` rows for grant, reactivation, role change, and
disablement.

Membership administration is exposed through owner-only platform API operations:

- grant staff membership to an existing active account by login email
- list active and inactive memberships for a business
- list immutable membership audit history
- update an active membership role between `STAFF` and `OWNER`
- disable a membership

All operations resolve caller authority server-side from active `OWNER` membership. JWTs remain
account-scoped and do not carry business role claims. Update and disable operations target
`membershipId` under the business route and collapse missing, wrong-business, and unauthorized
membership lookups into the same not-found style public response.

The service preserves at least one active owner per business. Re-granting a previously disabled
membership reactivates the existing current row as `STAFF`. Disabling an already inactive membership
returns the current inactive state without appending another disable audit entry.

## Consequences

- Request-time authorization keeps using current membership state without replaying audit history.
- Owners get an explicit access-review trail without mixing membership history with billing or
  payment history.
- Staff role changes and disablement affect the next server-side business access check without
  waiting for token expiration.
- Future invitation email delivery, staff acceptance flows, richer role taxonomy, and membership UI
  remain separate product work.

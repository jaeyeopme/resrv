# ADR-0004: Business And Membership Boundary

## Status

Accepted.

## Date

2026-05-25

## History

- `bdb4464 feat(business): add business membership model`

## Context

The original tenant/admin model tied organization ownership and user role to tenant-local
concepts. The redesign needs a platform-level organization boundary and explicit membership.

## Decision

Use:

- `Business` as the organization that owns booking configuration and resources.
- `BusinessMembership` as account-to-business access.
- `BusinessRole` values `OWNER` and `STAFF`.

Creating a business creates owner membership for the creator account.

## Alternatives

### Keep tenant terminology

Tenant is common in SaaS architecture, but the product-facing domain is clearer with Business.

### Put role claims in JWT

This avoids membership lookup on each business operation, but role claims become stale and can be
misused across businesses.

## Consequences

- Business authorization is a server-side lookup.
- Account identity and business access are separate.
- Timeslot can reference `business_id` without owning platform membership rules.

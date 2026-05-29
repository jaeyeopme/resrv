# ADR-0006: Account-Scoped JWT

## Status

Accepted.

## Date

2026-05-25

## History

- `3ee8941 feat(auth): issue account scoped tokens`
- `703287b fix(auth): reject malformed account tokens`

## Context

The old token model carried tenant and role context. The redesign separates identity from business
authorization, so tokens should identify the account only.

## Decision

Issue account-scoped JWTs.

Required token validation:

- `sub` is a UUID.
- `accountId` is a UUID.
- `sub` and `accountId` match for platform validation.
- `jti` is present.
- Issuer matches configured `resrv.jwt.issuer`.
- Audience contains configured `resrv.jwt.audience`.
- Token is within valid time range.
- HS256 secret is at least 32 bytes.

JWTs do not carry `businessId`, owner/staff role, customer/business actor authority, or any other
business-local authority.

## Alternatives

### Put Business Role Claims In JWT

This reduces database lookups but creates stale authorization and cross-business misuse risk.

### Session-Based Authentication

Sessions are viable for browser apps, but an API-first backend benefits from bearer tokens and
generated API review.

## Consequences

- Business authorization must query membership data.
- Timeslot can consume platform account tokens.
- Token validation failures are rejected before application use cases run.

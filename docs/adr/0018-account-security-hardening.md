# ADR-0018: Account Security Hardening

## Status

Accepted

## Context

The platform uses account-scoped JWTs. Tokens do not carry business identity, tenant-local role, or
business authorization claims. Production readiness requires repeated password failures to trigger
account recovery, and protected actions must stop when account, business, or membership state
changes before token expiration.

## Decision

Repeated failed password sign-ins are tracked as platform-owned, account-scoped security state. On
the fifth failed password attempt for an existing account, the platform creates a password reset
challenge, sends a password reset email through an outbound email port, and blocks password sign-in
for that account until reset succeeds.

Password reset tokens are generated as random secrets, stored only as digests, and consumed through
a public reset endpoint. Reset completion updates the account password hash, marks the challenge
used, and clears sign-in protection.

Protected authenticated requests recheck active account state at request time. Business-scoped
owner/staff checks require active account, active business, and active membership. Timeslot keeps
using explicit `platform-exchange` lookups through its outbound platform adapter.

Public generated documentation and public booking discovery remain reachable. Inactive businesses
or resources produce no bookable public discovery results.

## Consequences

- Account recovery does not affect business, resource, slot, or reservation availability for other
  accounts.
- Login failure security facts are committed even when authentication fails.
- Email delivery is replaceable: production uses SMTP-compatible Spring Mail, tests use a fake
  adapter.
- Password failure hardening uses reset-required recovery as the account-level control.
- Token revocation remains separate deferred hardening work.

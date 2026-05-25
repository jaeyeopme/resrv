# Research: Account Security Hardening

## Decision: Model repeated password failures as account-scoped sign-in protection

**Rationale**: The clarified requirement is account-scoped: 5 failed password sign-in attempts
require email verification and password reset. Storing this in platform keeps account security
separate from business/resource/slot availability and avoids accidental operational outages when
one owner account is protected.

**Alternatives considered**:
- Caller-only throttling: reduces abuse from one caller context but does not protect an account
  targeted from multiple sources.
- Business-scoped blocking: rejected because account sign-in protection must not affect booking
  availability or other members' access.

## Decision: Keep password reset challenge active until reset succeeds

**Rationale**: The product decision is that the protection requirement does not automatically
expire. Individual reset links may expire and be reissued, but password sign-in stays blocked until
the account owner resets the password through email.

**Alternatives considered**:
- Timed lockout: rejected because the project wants recovery through password reset, not passive
  waiting.
- Permanent administrative unlock only: rejected because it can strand legitimate solo operators.

## Decision: Send reset email on the fifth failed attempt

**Rationale**: Immediate delivery gives the legitimate account owner a direct recovery path and
matches the clarified flow. Responses must remain non-enumerating so unknown accounts do not leak
existence.

**Alternatives considered**:
- Send only after correct password is later supplied: rejected after clarification.
- Require manual "send email" request only: rejected because it adds unnecessary friction.

## Decision: Add a platform outbound email port with SMTP delivery adapter

**Rationale**: Email delivery is part of this feature, but provider choice should not leak into
domain or application code. Use a platform application port with an SMTP-compatible Spring Mail
adapter for delivery and an in-memory fake adapter in integration tests.

**Alternatives considered**:
- Hard-code a provider SDK in the service: rejected because it couples application logic to
  infrastructure.
- Log-only delivery as the only implementation: rejected because the feature requires delivery,
  though tests may use a fake adapter.

## Decision: Add password reset token storage separate from account identity

**Rationale**: Reset links need single-use verification, expiration, and auditability without
changing account identity or token claims. Storing a digest of the reset token prevents raw reset
secrets from being persisted.

**Alternatives considered**:
- Encode all reset state in the link: rejected because single-use invalidation and reissue tracking
  become weaker.
- Store raw reset tokens: rejected because a database leak would expose active recovery links.

## Decision: Enforce active account checks at request time

**Rationale**: Account-scoped tokens remain valid until expiration, so protected actions need a
server-side active account decision on each authenticated request. This implements the deferred
active-state hardening without adding account state to token claims.

**Alternatives considered**:
- Shorten token lifetime only: rejected because disabled accounts could still act until expiry.
- Add account status claims to tokens: rejected by the account-scoped token model and constitution.

## Decision: Preserve public discovery reachability while filtering inactive bookable results

**Rationale**: Generated docs and public booking discovery must remain reachable. Inactive
businesses/resources should not appear as bookable results, and one owner's sign-in protection must
not affect customers or other active members.

**Alternatives considered**:
- Require authentication for inactive business discovery: rejected because endpoint reachability
  should not change.
- Return business operational details publicly: rejected because bookability is enough for public
  discovery.

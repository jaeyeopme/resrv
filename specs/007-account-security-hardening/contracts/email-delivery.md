# Contract: Email Delivery

## Purpose

Deliver a password reset link to the registered email address immediately after an existing account
reaches 5 failed password sign-in attempts.

## Producer

Platform authentication application service through an outbound email port.

## Consumer

Account owner at the registered email address.

## Delivery Adapter

Production delivery uses an SMTP-compatible adapter. Tests may replace it with an in-memory fake
adapter that records deliveries without contacting an external provider.

## Message

**Template purpose**: password reset required after repeated failed sign-in attempts.

**Required fields**
- Recipient email address
- Reset link
- Expiration timestamp for the current link
- Support-safe explanatory text that does not include raw tokens in logs

**Required semantics**
- Email is sent when the fifth failed password attempt occurs.
- The reset link opens the password reset flow.
- The account remains unable to sign in with any password until reset succeeds.
- If the link expires, a replacement link may be issued while the password reset requirement remains.

## Security Requirements

- Raw reset tokens must not be logged.
- Raw passwords must not be included in any email content.
- Unknown-account attempts must not send email.
- Public sign-in responses must not reveal whether an email was sent.

## Test Contract

Integration tests may use a fake email adapter that records:
- recipient
- template purpose
- link target
- delivery time

Tests must verify that the fifth failed password attempt produces exactly one expected delivery for
the account and that unrelated accounts are not affected.

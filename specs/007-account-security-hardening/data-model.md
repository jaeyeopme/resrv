# Data Model: Account Security Hardening

## Account

Existing platform identity.

**Existing fields used**
- `id`
- `email`
- `hashedPassword`
- `status`

**New behavior**
- Password sign-in is rejected when the account has an active email verification challenge.
- Password reset completion replaces `hashedPassword` and clears failed-attempt blocking state.
- `status != ACTIVE` denies protected authenticated actions at request time.

## SignInAttempt

Security-relevant record of a password sign-in attempt.

**Fields**
- `id`: unique attempt identifier
- `accountId`: account identifier when the email resolves to an account; absent for unknown email
- `emailHash`: normalized email digest for non-enumerating investigation and repeated unknown-email tracking
- `callerFingerprint`: coarse caller context used for abuse investigation
- `outcome`: `FAILED_UNKNOWN_ACCOUNT`, `FAILED_BAD_PASSWORD`, `FAILED_REQUIRES_RESET`, `SUCCESS`
- `occurredAt`: attempt time

**Validation**
- Raw passwords are never stored.
- Raw reset tokens are never stored.
- Unknown-account attempts must not expose account existence through response differences.

## AccountSignInProtection

Current account-scoped protection state.

**Fields**
- `accountId`: protected account identifier
- `failedPasswordAttempts`: count of consecutive failed password attempts since the last successful sign-in or reset
- `verificationRequired`: whether password sign-in is blocked until reset succeeds
- `verificationRequiredAt`: time the fifth failed attempt triggered the requirement
- `lastFailedAt`: most recent failed password attempt time
- `updatedAt`: last state update time

**State transitions**
- `NONE` -> `COUNTING`: first failed password attempt for an existing account.
- `COUNTING` -> `REQUIRES_PASSWORD_RESET`: fifth failed password attempt; reset email is sent immediately.
- `COUNTING` -> `NONE`: successful sign-in before the fifth failed attempt.
- `REQUIRES_PASSWORD_RESET` -> `NONE`: password reset succeeds.

**Rules**
- Protection is account-scoped only.
- Protection must not change business, resource, slot, or reservation availability.
- Protection must not block unrelated accounts from signing in or operating the same business.

## PasswordResetChallenge

Single-use recovery challenge delivered through email after repeated failed password attempts.

**Fields**
- `id`: unique challenge identifier
- `accountId`: protected account identifier
- `tokenDigest`: digest of the reset token
- `reason`: `FAILED_PASSWORD_ATTEMPTS`
- `createdAt`: creation time
- `expiresAt`: link expiration time
- `usedAt`: time the reset token was consumed
- `replacedAt`: time a newer challenge superseded this one, if any

**State transitions**
- `ACTIVE` -> `USED`: password reset succeeds.
- `ACTIVE` -> `EXPIRED`: current time is after `expiresAt`.
- `ACTIVE` -> `REPLACED`: a new reset link is issued.

**Rules**
- The verification requirement remains until password reset succeeds, even if a link expires.
- Expired links may be reissued.
- Only the latest active challenge should be accepted for reset.

## ActiveAccountDecision

Request-time decision for authenticated platform accounts.

**Inputs**
- Acting `accountId`
- Current account status
- Endpoint/publicness classification

**Outcomes**
- `ALLOW`: account is active or endpoint is public.
- `DENY_INACTIVE_ACCOUNT`: protected authenticated action is attempted by an inactive account.

**Rules**
- Documentation endpoints remain public.
- Platform public endpoints remain available according to the security policy.
- Protected authenticated actions require an active account.

## ActiveBusinessAccessDecision

Request-time decision for business-scoped owner/staff actions.

**Inputs**
- Acting `accountId`
- `businessId`
- Account status
- Business status
- Business membership active flag and role

**Outcomes**
- `ALLOW`: active account, active business, active owner/staff membership.
- `DENY_INACTIVE_ACCOUNT`
- `DENY_INACTIVE_BUSINESS`
- `DENY_INACTIVE_OR_INSUFFICIENT_MEMBERSHIP`

**Rules**
- Timeslot continues to receive this decision through explicit platform contract types.
- Timeslot must not read platform tables directly.
- Customer reservation ownership checks remain separate from owner/staff membership checks.

## PublicBookingDiscovery

Public read-only resource/slot discovery for customers.

**Inputs**
- `businessId`
- `resourceId` where applicable
- Business active status
- Resource active status

**Rules**
- Public endpoints remain reachable.
- Inactive businesses/resources are excluded from bookable results.
- Account-level sign-in protection state is not an input and must not affect results.

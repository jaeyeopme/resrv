# Contract Delta: Generated OpenAPI

This file describes the feature-specific generated contract changes. It is not a hand-maintained
endpoint catalog.

## Sign-In Behavior

`POST /api/auth/login` keeps the existing successful response shape.

New failure behavior:
- Invalid or unknown credentials remain non-enumerating.
- The fifth failed password sign-in attempt for an existing account triggers email delivery to the
  registered email address.
- Accounts requiring email verification cannot sign in with any password until password reset
  succeeds.
- Responses must not reveal whether the submitted email belongs to an account.

Expected generated contract coverage:
- Unauthorized sign-in failure.
- Retry/recovery-required problem response for accounts requiring password reset.
- No raw password or reset token appears in response examples.

## Password Reset Behavior

Generated contract must expose the password reset flow needed by the email link.

Required behavior:
- Accept a reset token and new password.
- Reject expired, replaced, malformed, or already used reset tokens.
- Clear the account's sign-in protection state when reset succeeds.
- Return a non-sensitive success response.

Expected generated contract coverage:
- Success.
- Invalid token.
- Expired/replaced/used token.
- Password validation failure.

## Protected Authenticated Actions

All protected authenticated actions must deny inactive accounts at request time.

Expected generated contract coverage:
- Protected endpoints still require authentication.
- Inactive accounts receive a stable authorization failure.
- Public endpoints remain public.

## Business-Scoped Owner/Staff Actions

Business-scoped owner/staff actions must require:
- active acting account
- active business
- active owner/staff membership

Expected generated contract coverage:
- inactive business denial
- inactive membership denial
- insufficient role denial where applicable
- no business role or business identity is accepted from token claims

## Public Booking Discovery

Public discovery remains reachable for:
- resource list
- slot list

Required behavior:
- inactive businesses do not expose bookable resource/slot results
- inactive resources do not expose bookable slot results
- account-level sign-in protection does not affect anonymous discovery

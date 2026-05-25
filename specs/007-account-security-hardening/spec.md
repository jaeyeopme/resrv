# Feature Specification: Account Security Hardening

**Feature Branch**: `007-account-security-hardening`
**Created**: 2026-05-26
**Status**: Draft
**Input**: User description: "Harden account security for production readiness by adding sign-in throttling, email verification after repeated failed sign-ins, and request-time active account/business membership validation without changing the account-scoped token model."

## Clarifications

### Session 2026-05-26

- Q: What should happen after repeated failed password sign-in attempts reach the account-level protection threshold? → A: Require email verification after the threshold, including email delivery in this feature.
- Q: How many failed password sign-in attempts should trigger email verification, and should the verification requirement expire automatically? → A: Require email verification after 5 failed attempts; the verification requirement remains until password reset succeeds.
- Q: When should verification email be sent after repeated failures, and what should the link do? → A: Send the email immediately after the fifth failed attempt; the link opens password reset, and sign-in stays blocked until reset succeeds.
- Q: Should one owner's sign-in protection state affect public booking availability or other members' access? → A: No; sign-in protection is account-scoped and must not change business, resource, slot, reservation availability, or other members' access.
- Q: How should public discovery behave for inactive businesses or resources? → A: Public endpoints remain reachable, but inactive businesses or resources are not exposed as bookable results.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Slow Repeated Failed Sign-In Attempts (Priority: P1)

As a platform operator, I need repeated failed sign-in attempts to be slowed and eventually blocked so that password guessing does not remain cheap or invisible.

**Why this priority**: Sign-in is the public write surface with the highest abuse risk. Hardening it improves production readiness without changing the product model.

**Independent Test**: Can be tested by attempting repeated failed sign-ins for the same account and confirming that normal attempts are accepted until the fifth failure and then require email verification.

**Acceptance Scenarios**:

1. **Given** an existing account with no recent failed sign-in attempts, **When** the user enters an incorrect password, **Then** the attempt is rejected without revealing whether the account exists.
2. **Given** an account has 5 failed password sign-in attempts, **When** the fifth failure occurs, **Then** the system immediately sends a verification email to the registered email address.
3. **Given** the user signs in successfully before an email verification challenge is required, **When** the same account signs in again with correct credentials, **Then** the account can proceed normally.

---

### User Story 2 - Require Email Verification After Suspicious Failures (Priority: P2)

As an account owner, I need email verification required after suspicious failed sign-in attempts so that an attacker cannot keep guessing indefinitely without proving access to my email.

**Why this priority**: Account-level email verification protects specific accounts even when attempts come from changing caller contexts.

**Independent Test**: Can be tested by causing 5 failed password sign-in attempts for one account and confirming that password sign-in remains blocked until password reset succeeds while unrelated accounts are not blocked.

**Acceptance Scenarios**:

1. **Given** an account reaches 5 failed password sign-in attempts, **When** the account tries to sign in with the correct password before completing password reset from the verification email, **Then** the sign-in is rejected and the user is guided to verify email access.
2. **Given** an account opens the verification email link, **When** the account owner completes password reset, **Then** sign-in succeeds with the new password and failure counters no longer block access.
3. **Given** an account requires email verification, **When** another account signs in from the same caller context, **Then** that unrelated account is evaluated independently.
4. **Given** an owner account requires email verification, **When** customers or other active members use booking flows for the same business, **Then** their access and booking availability are evaluated independently from the protected owner account.

---

### User Story 3 - Recheck Active Access On Protected Actions (Priority: P3)

As a business owner or operator, I need protected actions to recheck active account, business, and membership state at request time so that disabled access stops working even if a token has not expired.

**Why this priority**: Tokens intentionally carry account identity only. Production readiness requires server-side state to remain authoritative after account, business, or membership changes.

**Independent Test**: Can be tested by signing in, changing account/business/membership state, and confirming that protected business actions follow the latest active-state decision without requiring token changes.

**Acceptance Scenarios**:

1. **Given** a signed-in account becomes inactive, **When** it attempts any protected action, **Then** the action is rejected.
2. **Given** a business becomes inactive, **When** an owner or staff member attempts a business-scoped protected action, **Then** the action is rejected.
3. **Given** a membership becomes inactive or loses sufficient role access, **When** the account attempts a business-scoped protected action, **Then** the action is rejected.

### Edge Cases

- Failed attempts for unknown accounts must not reveal whether the account exists.
- A successful sign-in after temporary failures must clear only the appropriate failed-attempt state for that account and caller context.
- Email verification must not permanently strand legitimate users; password reset links may expire and be reissued while the verification requirement remains until reset succeeds.
- Excessive attempts must receive a stable, non-sensitive retry-later outcome instead of stack traces or inconsistent errors.
- Active-state rechecks must not block generated documentation or public read-only booking discovery endpoints.
- Public read-only booking discovery endpoints must remain reachable, but inactive businesses or resources must not appear as bookable results.
- Account-level sign-in protection must not change business, resource, slot, or reservation availability for other accounts.
- Business membership rechecks must continue to distinguish owner/staff access from customer reservation ownership.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST require email verification after 5 failed password sign-in attempts for the same account.
- **FR-002**: System MUST continue using non-enumerating sign-in responses so failed attempts do not disclose whether an account exists.
- **FR-003**: System MUST immediately send a verification email to the account's registered email address when the fifth failed password sign-in attempt occurs.
- **FR-004**: System MUST direct the verification email link to a password reset flow.
- **FR-005**: System MUST keep the email verification requirement active until password reset succeeds.
- **FR-006**: System MUST reject password sign-in for an account that requires email verification until password reset succeeds.
- **FR-007**: System MUST clear failed-attempt blocking state after successful sign-in when email verification is not required, or after required password reset succeeds.
- **FR-008**: System MUST make retry-later outcomes understandable to legitimate clients without exposing sensitive security details.
- **FR-009**: System MUST evaluate active account status before allowing protected authenticated actions.
- **FR-010**: System MUST evaluate active business status before allowing protected business-scoped write or operations actions.
- **FR-011**: System MUST evaluate active business membership and sufficient role access before allowing protected owner/staff business actions.
- **FR-012**: System MUST preserve account-scoped tokens that do not contain business identity, business role, or customer/business actor authority.
- **FR-013**: System MUST record enough security-relevant facts for operators to investigate repeated failed sign-ins and active-state denials without storing raw credentials.
- **FR-014**: Public documentation endpoints and public read-only booking discovery MUST remain available according to the existing security policy.
- **FR-015**: Public read-only booking discovery MUST exclude inactive businesses and inactive resources from bookable results.
- **FR-016**: Account-level sign-in protection MUST NOT change business, resource, slot, or reservation availability for other accounts.
- **FR-017**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.
- **FR-018**: User-visible behavior changes MUST be reflected in the generated contract surface and covered by acceptance-level tests.

### Key Entities

- **Sign-In Attempt**: A security-relevant attempt to authenticate an account, including outcome, time, account reference when known, and caller context.
- **Email Verification Challenge**: Account protection state that prevents password sign-in after 5 failed attempts until the account owner resets the password through a delivered email link.
- **Active Access Decision**: The request-time decision that combines the acting account's status, business status, membership status, role access, and reservation ownership where applicable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of accounts with 5 failed password sign-in attempts require email verification before another password sign-in can succeed.
- **SC-002**: 100% of accounts requiring email verification are prevented from signing in with any password until password reset succeeds.
- **SC-003**: 100% of protected authenticated actions are denied for inactive accounts in acceptance tests.
- **SC-004**: 100% of protected business-scoped owner/staff actions are denied when the business or required membership is inactive in acceptance tests.
- **SC-005**: Public generated documentation and public booking discovery remain accessible in regression tests after the hardening changes.
- **SC-006**: Security rejection responses avoid account-existence disclosure in all sign-in failure scenarios covered by tests.
- **SC-007**: 100% of account-level sign-in protection scenarios leave unrelated accounts and public booking availability unaffected in acceptance tests.

## Assumptions

- Existing account registration and sign-in remain the primary authentication flow.
- Account-scoped tokens remain valid until expiration, but protected actions rely on request-time server-side access decisions.
- Individual password reset links may expire and be reissued, but the verification requirement itself remains until password reset succeeds.
- Email verification delivery is included in this feature; a full administration UI remains separate.
- Public read-only booking discovery remains intentionally public unless a later product decision changes that policy.

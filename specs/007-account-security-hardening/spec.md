# Feature Specification: Account Security Hardening

**Feature Branch**: `007-account-security-hardening`
**Created**: 2026-05-26
**Status**: Draft
**Input**: User description: "Harden account security for production readiness by adding sign-in throttling, failed sign-in lockout, and request-time active account/business membership validation without changing the account-scoped token model."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Slow Repeated Failed Sign-In Attempts (Priority: P1)

As a platform operator, I need repeated failed sign-in attempts to be slowed and eventually blocked so that password guessing does not remain cheap or invisible.

**Why this priority**: Sign-in is the public write surface with the highest abuse risk. Hardening it improves production readiness without changing the product model.

**Independent Test**: Can be tested by attempting repeated failed sign-ins for the same account and caller context, then confirming that normal attempts are accepted until the defined threshold and excessive attempts receive a controlled rejection.

**Acceptance Scenarios**:

1. **Given** an existing account with no recent failed sign-in attempts, **When** the user enters an incorrect password, **Then** the attempt is rejected without revealing whether the account exists.
2. **Given** repeated failed sign-in attempts within the protected window, **When** the caller continues trying to sign in, **Then** the system rejects excessive attempts with a clear retry-later outcome.
3. **Given** the protected window has passed or the user signs in successfully, **When** the same account signs in again with correct credentials, **Then** the account can proceed normally unless it is locked by policy.

---

### User Story 2 - Lock Accounts After Suspicious Failures (Priority: P2)

As an account owner, I need my account protected after suspicious failed sign-in attempts so that an attacker cannot keep guessing indefinitely.

**Why this priority**: Account-level lockout protects specific accounts even when attempts come from changing caller contexts.

**Independent Test**: Can be tested by triggering the failed-attempt threshold for one account and confirming that correct credentials remain blocked during the lock period while unrelated accounts are not locked.

**Acceptance Scenarios**:

1. **Given** an account reaches the failed-attempt threshold, **When** the account tries to sign in with the correct password before the lock expires, **Then** the sign-in is rejected with a retry-later outcome.
2. **Given** an account lock expires, **When** the account owner signs in with the correct password, **Then** sign-in succeeds and failure counters no longer block access.
3. **Given** an account is locked, **When** another account signs in from the same caller context, **Then** that unrelated account is evaluated independently.

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
- Lockout must not permanently strand legitimate users after the configured lock period.
- Excessive attempts must receive a stable, non-sensitive retry-later outcome instead of stack traces or inconsistent errors.
- Active-state rechecks must not block generated documentation or public read-only booking discovery endpoints.
- Business membership rechecks must continue to distinguish owner/staff access from customer reservation ownership.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST limit repeated failed sign-in attempts for the same account and caller context within a short protected window.
- **FR-002**: System MUST continue using non-enumerating sign-in responses so failed attempts do not disclose whether an account exists.
- **FR-003**: System MUST temporarily lock an account after the account reaches the failed sign-in threshold.
- **FR-004**: System MUST reject sign-in for a locked account until the lock period expires or an authorized administrative recovery path clears it.
- **FR-005**: System MUST clear failed-attempt blocking state after a successful sign-in when the account is not locked.
- **FR-006**: System MUST make retry-later outcomes understandable to legitimate clients without exposing sensitive security details.
- **FR-007**: System MUST evaluate active account status before allowing protected authenticated actions.
- **FR-008**: System MUST evaluate active business status before allowing protected business-scoped write or operations actions.
- **FR-009**: System MUST evaluate active business membership and sufficient role access before allowing protected owner/staff business actions.
- **FR-010**: System MUST preserve account-scoped tokens that do not contain business identity, business role, or customer/business actor authority.
- **FR-011**: System MUST record enough security-relevant facts for operators to investigate repeated failed sign-ins and active-state denials without storing raw credentials.
- **FR-012**: Public documentation endpoints and public read-only booking discovery MUST remain available according to the existing security policy.
- **FR-013**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.
- **FR-014**: User-visible behavior changes MUST be reflected in the generated contract surface and covered by acceptance-level tests.

### Key Entities

- **Sign-In Attempt**: A security-relevant attempt to authenticate an account, including outcome, time, account reference when known, and caller context.
- **Account Lock**: Temporary account protection state that prevents sign-in until a lock period ends or an authorized recovery action clears it.
- **Active Access Decision**: The request-time decision that combines account status, business status, membership status, role access, and reservation ownership where applicable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of excessive failed sign-in attempts within the protected window receive a retry-later outcome instead of continuing normal password checks.
- **SC-002**: 100% of locked accounts are prevented from signing in with correct credentials until the lock period expires or recovery clears the lock.
- **SC-003**: 100% of protected authenticated actions are denied for inactive accounts in acceptance tests.
- **SC-004**: 100% of protected business-scoped owner/staff actions are denied when the business or required membership is inactive in acceptance tests.
- **SC-005**: Public generated documentation and public booking discovery remain accessible in regression tests after the hardening changes.
- **SC-006**: Security rejection responses avoid account-existence disclosure in all sign-in failure scenarios covered by tests.

## Assumptions

- Existing account registration and sign-in remain the primary authentication flow.
- Account-scoped tokens remain valid until expiration, but protected actions rely on request-time server-side access decisions.
- Thresholds and lock durations use conservative production defaults that can be adjusted later without changing user-facing policy.
- Administrative unlock or recovery can be represented as a policy requirement even if a full administration UI is delivered separately.
- Public read-only booking discovery remains intentionally public unless a later product decision changes that policy.

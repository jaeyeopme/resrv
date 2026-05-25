# Feature Specification: Platform Account And Authentication Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0003, ADR-0005, ADR-0006, and security documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register Platform Account (Priority: P1)

A new person creates a platform account with email, name, and password so they
can later act as a business owner, staff member, or customer.

**Why this priority**: Account identity is the entry point for all authenticated
platform and booking workflows.

**Independent Test**: Submit valid account details and verify the account is
created with returned identity details but without exposing password data.

**Acceptance Scenarios**:

1. **Given** a unique email, valid name, and valid password, **When** the person registers, **Then** the system creates an active account and returns account id, email, and name.
2. **Given** an email already owned by an existing account, **When** registration is submitted, **Then** the system rejects the request as a duplicate account.
3. **Given** missing, blank, or invalid account fields, **When** registration is submitted, **Then** the system rejects the request before creating an account.

---

### User Story 2 - Login With Account Credentials (Priority: P1)

An account holder logs in with email and password to receive a bearer token that
identifies only their platform account.

**Why this priority**: Authenticated API use depends on a token that separates
identity from business authorization.

**Independent Test**: Register an account, log in with the same credentials, and
verify the response contains an access token and expiration interval.

**Acceptance Scenarios**:

1. **Given** an active account and matching password, **When** login is submitted, **Then** the system returns an access token and expiration interval.
2. **Given** an unknown email, inactive account, blank credential field, or wrong password, **When** login is submitted, **Then** the system returns the same invalid-credentials result without revealing which field failed.

---

### User Story 3 - Use Account-Scoped Token (Priority: P2)

An authenticated caller uses a token that proves account identity but does not
grant business role or reservation actor authority by itself.

**Why this priority**: Downstream platform and timeslot workflows rely on a
safe identity token and server-side authorization.

**Independent Test**: Use a valid token on a protected endpoint and verify the
account id is accepted, then use malformed or mismatched account claims and
verify access is rejected.

**Acceptance Scenarios**:

1. **Given** a valid account-scoped token, **When** the caller accesses a protected endpoint, **Then** the system authenticates the caller as the token account.
2. **Given** a token with missing token id, malformed account id, invalid issuer, invalid audience, expiration, or mismatched subject/account id, **When** the token is used, **Then** the system rejects the request.
3. **Given** a token that includes business role or business id claims, **When** the caller attempts a business operation, **Then** business access is still resolved server-side and is not trusted from token claims.

### Edge Cases

- Duplicate account email with different letter case or leading/trailing spaces must not create a second logical identity.
- Passwords must never be returned in account or login responses.
- Public API documentation must remain reachable without a token.
- Authentication failure responses must not reveal whether the email, password, active state, or input shape caused the failure.
- JWT secret, issuer, audience, and expiration must be configured for the running environment.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow account registration with email, display name, and password.
- **FR-002**: The system MUST reject account registration when email, name, or password is missing, blank, malformed, or outside accepted bounds: name 1-100 characters after trimming, password 8-72 characters, and email matching platform email rules.
- **FR-003**: The system MUST trim and lowercase account emails before duplicate checks and persistence.
- **FR-004**: The system MUST store only password hashes, never raw passwords.
- **FR-005**: The system MUST allow active accounts to login with valid email and password.
- **FR-006**: The system MUST return the same invalid-credentials outcome for unknown email, inactive account, invalid password, and invalid credential input.
- **FR-007**: Login success MUST return exactly an access token and expiration interval to the caller.
- **FR-008**: Issued tokens MUST identify the platform account and include subject, account id, token id, issuer, audience, issued time, and expiration.
- **FR-009**: Issued tokens MUST NOT grant business role, business id, tenant-local role, or reservation actor authority.
- **FR-010**: Protected platform endpoints MUST reject tokens with missing token id, malformed subject, malformed account id, mismatched subject/account id, invalid issuer, invalid audience, or expiration.
- **FR-011**: Public account registration, login, and generated API documentation endpoints MUST be available without an existing token.
- **FR-012**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-013**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **Account**: Platform identity with normalized email, display name, hashed password, active status, and creation timestamp.
- **Access Token**: Bearer credential that identifies an account for protected API requests.
- **Credential Failure**: Uniform authentication failure outcome for invalid login attempts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can trace account registration, login, and token validation behavior from this baseline to accepted ADRs without reading implementation code.
- **SC-002**: 100% of account and login acceptance scenarios above are covered by either current tests or identified follow-up test gaps before behavior changes are planned.
- **SC-003**: No documented login failure path exposes whether the account, password, active state, or input shape failed.
- **SC-004**: No documented token behavior grants business access without a server-side membership or ownership check.

## Assumptions

- This is a baseline specification for existing implemented behavior, not a request to add new behavior.
- Account activity is represented as active/inactive status in the platform account model.
- Login rate limiting, failed-login lockout, logout token revocation, and token blacklist are deferred hardening items.
- Generated OpenAPI remains the API contract; this spec does not enumerate every response field beyond behavior required for the baseline.

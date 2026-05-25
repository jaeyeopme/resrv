# Feature Specification: Business And Membership Authorization Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0004, ADR-0013, and security documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create A Business As Owner (Priority: P1)

An authenticated account creates a business so the account becomes the owner of
that business and can manage business-owned booking behavior.

**Why this priority**: Business ownership is the boundary for platform setup and
all business-scoped booking operations.

**Independent Test**: Use an authenticated account to create a business and
verify the business exists with owner access for the creator.

**Acceptance Scenarios**:

1. **Given** an authenticated account and valid business details, **When** the account creates a business, **Then** the system creates the business and owner membership for that account.
2. **Given** an existing business slug, **When** another create request uses the same slug, **Then** the system rejects the duplicate slug.
3. **Given** missing, blank, or invalid business fields, **When** business creation is submitted, **Then** the system rejects the request before creating a business.

---

### User Story 2 - Authorize Business Operations (Priority: P1)

A business owner or staff member performs business-scoped actions only when
their account has active membership for that business.

**Why this priority**: Business data isolation depends on server-side membership
checks rather than trusted client claims.

**Independent Test**: Try a business-scoped operation as an active member and as
a non-member, and verify only the active member can proceed.

**Acceptance Scenarios**:

1. **Given** an account with active owner or staff membership, **When** the account performs a business-scoped operation, **Then** the system allows the operation.
2. **Given** an account with no membership, inactive membership, or membership for a different business, **When** the account performs the operation, **Then** the system denies access.
3. **Given** a token containing business role claims, **When** the account lacks active membership, **Then** the system denies access.

---

### User Story 3 - Preserve Platform/Timeslot Boundary (Priority: P2)

Timeslot workflows need business status and membership decisions without owning
platform account, business, or membership persistence rules.

**Why this priority**: The architecture requires explicit bounded-context
contracts and prevents direct platform table coupling from timeslot.

**Independent Test**: Review a timeslot business access flow and verify the
decision comes from an explicit platform contract rather than client claims or
platform table reads.

**Acceptance Scenarios**:

1. **Given** a timeslot workflow requiring business access, **When** access is checked, **Then** the decision is resolved through a platform-owned membership contract.
2. **Given** a timeslot workflow for an inactive or missing business, **When** the workflow validates business context, **Then** the workflow rejects the operation.

### Edge Cases

- An inactive membership must not authorize any business operation.
- A membership for one business must not authorize another business.
- Business role claims in tokens must not bypass membership lookup.
- Business creation must not create duplicate slugs.
- Staff membership administration is not part of the current baseline.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow an authenticated account to create a business with valid business details: name 1-100 characters, slug 3-63 lowercase URL characters, and non-blank valid timezone.
- **FR-002**: Creating a business MUST create active owner membership for the creator account.
- **FR-003**: The system MUST prevent duplicate business slugs.
- **FR-004**: Business-scoped operations MUST require active owner or staff membership unless the operation is explicitly public read access.
- **FR-005**: Membership checks MUST be server-side and MUST NOT trust business id or role claims supplied by the client or token.
- **FR-006**: Timeslot workflows MUST access platform business and membership decisions through explicit platform contracts.
- **FR-007**: Timeslot workflows MUST NOT read platform-owned tables directly.
- **FR-008**: Workflows that load active business context MUST reject missing or inactive businesses.
- **FR-009**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-010**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **Business**: Organization with name, slug, timezone, and active state that owns booking settings, resources, schedules, and reservations.
- **BusinessMembership**: Account-to-business access record with active state and role.
- **BusinessRole**: Owner or staff role for business operations.
- **Business Access Decision**: Server-side result that allows or denies business-scoped action.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can trace business creation and owner membership behavior from this baseline to accepted ADRs without reading implementation code.
- **SC-002**: 100% of business-scoped authorization scenarios above are either covered by current tests or listed as test gaps before behavior changes are planned.
- **SC-003**: No baseline requirement allows business access from token role claims alone.
- **SC-004**: No baseline requirement allows timeslot to depend directly on platform persistence schema.

## Assumptions

- This is a baseline specification for existing implemented behavior, not a request to add membership administration.
- Owner and staff are the current business roles.
- Staff invitation and full membership management remain out of current scope.
- Public read endpoints are handled by the relevant API boundary spec.

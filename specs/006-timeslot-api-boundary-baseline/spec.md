# Feature Specification: Timeslot Booking API Boundary Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0014, ADR-0015, ADR-0016, and security documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Review API Contract (Priority: P1)

An API reviewer or integrator inspects generated documentation to understand
the platform and booking surface without needing credentials first.

**Why this priority**: Reviewability is a project goal and generated OpenAPI is
the API contract surface.

**Independent Test**: Access generated documentation endpoints without a token
and verify application mutation endpoints remain protected according to their
rules.

**Acceptance Scenarios**:

1. **Given** no token, **When** a reviewer opens generated API documentation, **Then** the documentation is accessible.
2. **Given** no token, **When** a reviewer attempts a protected application operation, **Then** the system rejects the request.
3. **Given** generated documentation, **When** an API behavior changes, **Then** the documentation reflects that behavior through generated contract output.

---

### User Story 2 - Use Public Booking Discovery (Priority: P1)

A customer or integrator discovers public booking options for businesses
without receiving mutation authority.

**Why this priority**: Customers need to view resources and slots before
creating a hold, while protected actions remain authenticated.

**Independent Test**: List resources and slots for an active business/resource
without a token, then attempt hold or mutation without a token and verify it is
rejected.

**Acceptance Scenarios**:

1. **Given** a business id with active resources, **When** public resources are listed, **Then** active resources are returned.
2. **Given** an active business, active resource, schedule, and settings, **When** public slots are listed, **Then** virtual slots are returned.
3. **Given** no token, **When** the caller attempts hold, confirm, release, cancel, setup, or business operations, **Then** the system rejects the protected operation.

---

### User Story 3 - Keep Timeslot Boundary Explicit (Priority: P1)

Timeslot exposes booking workflows while using platform contracts for business
status and membership decisions.

**Why this priority**: The redesign replaces old tenant-local API boundaries
with platform and timeslot bounded contexts.

**Independent Test**: Review a booking workflow and verify timeslot owns the
booking behavior while platform owns account, business, and membership
decisions.

**Acceptance Scenarios**:

1. **Given** a booking workflow requiring business status, **When** timeslot validates the business, **Then** the status comes through an explicit platform contract.
2. **Given** a booking workflow requiring business membership, **When** timeslot checks access, **Then** the access decision comes through an explicit platform contract.
3. **Given** old tenant-local concepts, **When** reviewing source-of-truth docs and API behavior, **Then** the current platform/timeslot terminology is used instead.

### Edge Cases

- Generated documentation must not expose secrets or imply public mutation access.
- Public resource and slot discovery must not grant hold or reservation transition authority.
- Account registration, login, generated documentation, active resource listing, and virtual slot listing are the only public baseline surfaces.
- Timeslot must not read platform persistence tables directly.
- Old tenant/admin/customer terminology must not remain as the active API model.
- Timeslot runtime packaging remains an open technical decision and must not obscure the API boundary.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Generated API documentation endpoints MUST be publicly readable, including Swagger UI and generated OpenAPI JSON/YAML endpoints.
- **FR-002**: Public generated documentation MUST NOT make protected application mutation endpoints public.
- **FR-003**: Platform public endpoints MUST include account registration and login only.
- **FR-004**: Timeslot public read endpoints MUST include active resource listing and virtual slot listing only.
- **FR-005**: Timeslot protected operations MUST require account authentication and server-side authorization appropriate to the operation.
- **FR-006**: Timeslot booking workflows MUST expose booking settings, resources, schedules, slots, holds, confirmations, releases, cancellations, check-in, no-show, and business reservation listing/search.
- **FR-007**: Timeslot MUST obtain platform business and membership information through explicit platform contracts.
- **FR-008**: Timeslot MUST NOT depend on platform domain, platform application services, platform repositories, platform entities, or platform persistence schema.
- **FR-009**: Source-of-truth docs MUST describe the platform/timeslot redesign as the active API model rather than the superseded tenant-local API.
- **FR-010**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-011**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **Platform API Surface**: Account and business lifecycle behavior.
- **Timeslot API Surface**: Booking setup, discovery, reservation lifecycle, and operations behavior.
- **Generated API Contract**: Reviewable contract emitted from the running application.
- **Platform Contract**: Explicit cross-context interface used by timeslot for platform-owned decisions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can identify which endpoints are public documentation, public booking discovery, and protected application operations without reading implementation code.
- **SC-002**: 100% of API boundary scenarios above are covered by current tests or identified as test gaps before API boundary changes are planned.
- **SC-003**: No baseline requirement allows timeslot to read platform persistence tables directly.
- **SC-004**: No baseline requirement treats generated documentation access as public mutation access.
- **SC-005**: No baseline requirement keeps the old tenant-local API as an active source of truth.

## Assumptions

- This is a baseline specification for existing implemented behavior.
- Generated OpenAPI remains the contract surface; this baseline does not maintain a handwritten endpoint catalog.
- Runtime packaging for timeslot remains an open technical decision.
- Microservice deployment and backward compatibility with the old tenant-local API are out of scope.
- Request-time active-state validation beyond explicit active business lookups remains deferred.

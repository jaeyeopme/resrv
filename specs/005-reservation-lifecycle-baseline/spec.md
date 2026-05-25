# Feature Specification: Reservation Lifecycle And Locking Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0011, ADR-0012, ADR-0013, and testing documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hold A Generated Slot (Priority: P1)

A customer holds a listed slot so the slot is temporarily protected while the
customer confirms or releases it.

**Why this priority**: Hold creation is the concurrency-sensitive start of the
reservation lifecycle.

**Independent Test**: List a slot, hold it as a customer, and verify a second
active hold or confirmed reservation cannot take the same resource/time range.

**Acceptance Scenarios**:

1. **Given** a valid generated slot with no active blocker, **When** a customer holds the slot, **Then** the system creates a hold with an expiration time.
2. **Given** an unexpired hold, confirmed reservation, or checked-in reservation for the same resource/time range, **When** another customer tries to hold the slot, **Then** the system rejects the hold.
3. **Given** an expired hold for the same resource/time range, **When** another customer tries to hold the slot, **Then** the expired hold no longer blocks capacity.

---

### User Story 2 - Customer Reservation Transitions (Priority: P1)

A customer confirms, releases, or cancels only their own reservation according
to lifecycle rules.

**Why this priority**: Customer actions must protect ownership and cancellation
cutoff rules.

**Independent Test**: Perform each customer transition as the owner and as a
different account, then verify only the owner can proceed.

**Acceptance Scenarios**:

1. **Given** a customer-owned hold, **When** the customer confirms it before expiration, **Then** the reservation becomes confirmed.
2. **Given** a customer-owned hold, **When** the customer releases it, **Then** the reservation no longer blocks capacity.
3. **Given** a customer-owned confirmed reservation before the cancellation cutoff, **When** the customer cancels it, **Then** the reservation is cancelled by the customer.
4. **Given** a customer-owned confirmed reservation at or after the cancellation cutoff, **When** the customer cancels it, **Then** the system rejects the transition.
5. **Given** a reservation owned by another customer, **When** an account attempts confirm, release, or customer cancel, **Then** the system denies the transition.

---

### User Story 3 - Business Reservation Transitions (Priority: P1)

A business owner or staff member lists and manages reservations for operational
workflows such as cancel, check-in, and no-show.

**Why this priority**: Business operations require active membership and correct
time-based transition rules.

**Independent Test**: Perform business transitions as an active member and as a
non-member, then verify only the member can proceed and time rules are enforced.

**Acceptance Scenarios**:

1. **Given** active owner or staff membership, **When** the account lists business reservations for a business-local date, **Then** the system returns reservations for that business date.
2. **Given** active owner or staff membership, **When** the account cancels a held or confirmed reservation, **Then** the reservation is cancelled by the business.
3. **Given** a confirmed reservation at or after start time, **When** active owner or staff checks it in, **Then** the reservation is checked in.
4. **Given** a reservation after end time, **When** active owner or staff marks no-show, **Then** the reservation is marked no-show.
5. **Given** no active membership, **When** the account attempts a business transition, **Then** the system denies access.

### Edge Cases

- Expired holds must stop blocking capacity without a cleanup mutation.
- Released, cancelled, no-show, and expired holds must not block capacity.
- Check-in must not happen before start time.
- No-show must not happen before end time.
- Customer cancellation must happen strictly before the reservation start time minus the effective cancellation window.
- Derived reservation state must not become stale because time passes.
- Concurrent hold attempts for the same resource/time range must not both succeed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hold creation MUST validate the slot identifier against current business, active resource, schedule, timezone, effective policy, and current time.
- **FR-002**: Hold creation MUST reject malformed, stale, wrong-business, wrong-resource, or unavailable slots.
- **FR-003**: Hold expiration MUST be calculated from the effective booking policy hold time.
- **FR-004**: Active blockers MUST include unexpired holds, confirmed reservations, and checked-in reservations.
- **FR-005**: Released, cancelled, no-show, and expired holds MUST NOT block capacity.
- **FR-006**: Expired holds MUST stop blocking without requiring a cleanup mutation.
- **FR-007**: Concurrent hold attempts for the same resource/time range MUST be serialized or otherwise prevented from both succeeding.
- **FR-008**: Reservation state MUST be derived from timestamp facts rather than a stale persisted status for held or expired state.
- **FR-009**: Confirm, release, and customer cancel MUST require reservation ownership.
- **FR-010**: Customer cancellation MUST be allowed only when the current time is strictly before reservation start time minus the effective cancellation window.
- **FR-011**: Business cancel MUST be allowed only for held or confirmed reservations and MUST require active owner or staff membership.
- **FR-012**: Check-in MUST require a confirmed reservation, active owner or staff membership, and current time at or after reservation start time.
- **FR-013**: No-show MUST require a confirmed reservation, active owner or staff membership, and current time at or after reservation end time.
- **FR-014**: Business reservation listing MUST use the business-local date window and MAY filter by resource, customer account, and derived reservation state.
- **FR-015**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-016**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **Reservation**: Timestamp-fact record for hold, confirm, release, cancel, check-in, and no-show.
- **Active Blocker**: Reservation fact combination that prevents another hold for overlapping capacity.
- **Reservation State**: Derived read/use-time state from timestamp facts and current time.
- **Reservation Actor**: Customer owner or business owner/staff account performing a transition.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can identify exactly which reservation facts block capacity at a given time without reading implementation code.
- **SC-002**: 100% of transition authorization scenarios above are covered by current tests or identified as test gaps before lifecycle changes are planned.
- **SC-003**: No baseline requirement requires a cleanup job for expired holds to stop blocking capacity.
- **SC-004**: No baseline requirement permits customer-owned transitions by non-owner accounts.
- **SC-005**: No baseline requirement permits business transitions without active business membership.

## Assumptions

- This is a baseline specification for existing implemented behavior.
- Payments, deposits, notifications, reminders, and external calendar sync are out of scope.
- Active hold quotas and token revocation are deferred hardening items.
- Business-local date listing is covered here for lifecycle ownership and also by the API boundary baseline for HTTP surface clarity.

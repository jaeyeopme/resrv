# Feature Specification: Booking Settings And Policy Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0007, ADR-0008, and architecture documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Business Booking Defaults (Priority: P1)

A business owner or staff member configures booking defaults so generated slots
and reservations follow the business policy.

**Why this priority**: Booking defaults are required before resources can
resolve final slot and hold behavior.

**Independent Test**: Configure valid booking settings for an active business
and verify later resource and slot behavior can use those settings.

**Acceptance Scenarios**:

1. **Given** an active business and valid policy values, **When** settings are saved, **Then** the business has booking defaults for slot duration, hold time, cancellation window, and max advance booking.
2. **Given** invalid policy values, **When** settings are submitted, **Then** the system rejects them before saving.
3. **Given** a missing or inactive business, **When** settings are submitted, **Then** the system rejects the request.

---

### User Story 2 - Create Resource With Optional Overrides (Priority: P1)

A business owner or staff member with active membership creates a reservable resource that can use
business defaults or override selected booking policy values.

**Why this priority**: Businesses need one or more reservable units before
customers can discover and hold slots.

**Independent Test**: Create a resource after settings exist and verify it
resolves an effective policy from defaults plus overrides.

**Acceptance Scenarios**:

1. **Given** business booking settings exist, **When** a valid resource is created without overrides, **Then** the resource uses business defaults.
2. **Given** business booking settings exist, **When** a valid resource is created with selected overrides, **Then** the resource uses overrides where present and business defaults elsewhere.
3. **Given** missing business settings, duplicate resource slug, invalid name, invalid slug, invalid description, or invalid override values, **When** resource creation is submitted, **Then** the system rejects the request.

---

### User Story 3 - Resolve Effective Booking Policy (Priority: P2)

The booking workflow consistently resolves the final policy used for slot
generation, holds, and cancellation decisions.

**Why this priority**: Duplicated fallback logic can cause slot and reservation
behavior to diverge.

**Independent Test**: Compare resource default and override combinations and
verify the same effective policy is used across slot and reservation flows.

**Acceptance Scenarios**:

1. **Given** a resource with no overrides, **When** policy is resolved, **Then** all policy values come from business defaults.
2. **Given** a resource with partial overrides, **When** policy is resolved, **Then** overridden values come from the resource and all other values come from business defaults.

### Edge Cases

- Resource behavior must be unavailable when business settings are missing.
- Resource slugs must be unique within a business, not globally.
- Optional descriptions are trimmed; blank-only descriptions become absent, and descriptions over 500 characters after trimming are rejected.
- Effective policy fallback must not be duplicated across services.
- Max advance booking days remains business-level policy in the current baseline.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow accounts with active owner or staff membership to create or replace booking settings for an active business.
- **FR-002**: Booking settings MUST include slot duration, hold time, cancellation window, and max advance booking days.
- **FR-003**: Booking settings MUST reject values outside accepted business rules: slot duration 5-480 minutes in 5-minute increments, hold time 1-30 minutes, cancellation window 0-10080 minutes, and max advance booking 1-365 days.
- **FR-004**: Booking settings MUST require an active business.
- **FR-005**: The system MUST allow accounts with active owner or staff membership to create reservable resources for a business.
- **FR-006**: Resource creation MUST require existing business booking settings.
- **FR-007**: Resource creation MUST require name 1-100 characters, slug 3-63 lowercase URL characters, and slug uniqueness within the business.
- **FR-008**: Resources MAY override slot duration, hold time, and cancellation window using the same bounds as business booking settings.
- **FR-009**: Effective booking policy MUST resolve resource overrides against business defaults in one domain policy concept.
- **FR-010**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-011**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **BusinessBookingSettings**: Business-level booking defaults.
- **Resource**: Reservable unit owned by a business with name, slug, optional normalized description, active state, and optional booking overrides.
- **ResourceBookingOverride**: Optional policy value that replaces a business default for a resource.
- **EffectiveBookingPolicy**: Final policy used by slot and reservation workflows.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can determine which policy values are business defaults and which can be resource overrides without reading implementation code.
- **SC-002**: 100% of policy validation and fallback scenarios above are covered by current tests or identified as test gaps before policy changes are planned.
- **SC-003**: No baseline requirement permits resource policy fallback to be reimplemented independently by separate services.
- **SC-004**: No baseline requirement permits booking settings for an inactive or missing business, or resource creation without existing business settings.

## Assumptions

- This is a baseline specification for existing implemented behavior.
- Membership administration and staff invitation are out of scope.
- The product currently treats max advance booking days as a business-level setting.
- Public resource listing behavior is covered by the timeslot API boundary baseline.
- Request-time active-state validation beyond explicit active business lookups remains deferred.

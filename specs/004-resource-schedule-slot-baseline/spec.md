# Feature Specification: Resource Schedules And Virtual Slots Baseline

**Feature Branch**: `001-baseline-specs`
**Created**: 2026-05-26
**Status**: Baseline
**Input**: Existing behavior baseline from PRD, TRD, ADR-0009, ADR-0010, and architecture documentation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage Resource Availability (Priority: P1)

A business owner or staff member with active membership defines weekly and date-specific availability
for a resource so customers only see bookable times.

**Why this priority**: Slots cannot be generated correctly without business
local availability windows.

**Independent Test**: Replace weekly windows and date override windows for a
resource, then verify the expected availability drives slot output.

**Acceptance Scenarios**:

1. **Given** an active business and resource, **When** weekly availability windows are replaced, **Then** the resource uses the new weekly windows for matching dates.
2. **Given** a date override for a resource, **When** slots are requested for that date, **Then** the override replaces the weekly schedule for that date.
3. **Given** an empty window list for a weekly day or date override, **When** slots are requested for that day, **Then** the resource is treated as closed for that day.

---

### User Story 2 - Discover Virtual Slots (Priority: P1)

A customer lists slots for a resource and date so they can choose a server-
generated booking option rather than inventing times.

**Why this priority**: Customer booking begins with deterministic slot
discovery.

**Independent Test**: Request slots for a resource and date with known settings,
schedule, and timezone, then verify each returned slot belongs to that resource
and date.

**Acceptance Scenarios**:

1. **Given** an active resource with settings and availability, **When** slots are requested for an allowed date, **Then** the system returns deterministic virtual slots.
2. **Given** the same settings, schedule, resource, date, and timezone, **When** slots are requested multiple times, **Then** the same slot identities and time ranges are returned.
3. **Given** a date outside the allowed advance booking range, **When** slots are requested, **Then** the system returns no bookable options.
4. **Given** an inactive resource, missing active business, or missing settings, **When** slots are requested, **Then** the system rejects the request.

---

### User Story 3 - Use Opaque Slot Identity (Priority: P2)

A customer selects an opaque slot identifier so the server can later verify the
selected business, resource, start time, and end time before creating a hold.

**Why this priority**: Hold creation must revalidate a server-generated slot and
reject client-invented times.

**Independent Test**: Use a listed slot identifier in a hold flow and verify it
binds to the same business, resource, and time range.

**Acceptance Scenarios**:

1. **Given** a listed slot, **When** the customer submits the slot identifier for hold creation, **Then** the identifier can be validated against the same generated slot.
2. **Given** a malformed, stale, wrong-business, or wrong-resource slot identifier, **When** it is submitted for hold creation, **Then** the system rejects it.

### Edge Cases

- Date overrides replace weekly schedules instead of merging with them.
- Empty schedules represent closed availability.
- Schedule windows must start and end on the same local date and must not overlap.
- Schedule windows are interpreted in the business timezone.
- Slot rows are not persisted as independent records.
- Slot identifiers must not allow clients to bypass schedule or policy validation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow accounts with active owner or staff membership to replace weekly availability windows for a resource.
- **FR-002**: The system MUST allow accounts with active owner or staff membership to replace date-specific override windows for a resource.
- **FR-003**: A date override MUST replace the weekly schedule for that date.
- **FR-004**: Empty schedule windows MUST represent closed availability for that day or date.
- **FR-005**: Schedule windows MUST have start time before end time on the same local date and MUST NOT overlap within the same weekly day or date override.
- **FR-006**: The system MUST generate slots from business timezone, resource schedule, and effective booking policy.
- **FR-007**: Public slot listing MUST return an empty list for dates before the business-local current date or after the configured max advance booking range.
- **FR-008**: Slots MUST be virtual and MUST NOT require persisted slot rows.
- **FR-009**: Slot generation MUST be deterministic for the same business, resource, settings, schedule, date, and timezone.
- **FR-010**: Each slot identifier MUST bind to business, resource, start time, and end time.
- **FR-011**: Hold creation MUST reject malformed, stale, wrong-business, wrong-resource, or unavailable slot identifiers.
- **FR-012**: API behavior changes MUST be visible through generated OpenAPI and covered by API integration tests.
- **FR-013**: Authorization-sensitive behavior MUST resolve business access server-side through membership or reservation ownership.

### Key Entities *(include if feature involves data)*

- **Weekly Schedule**: Business-local availability windows for a resource and day of week.
- **Date Override**: Business-local availability windows for a resource and date that replace weekly availability.
- **Schedule Window**: Local start and end time range.
- **Slot**: Virtual bookable time range with opaque identity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can explain how weekly schedules, date overrides, timezone, and policy produce slots without reading implementation code.
- **SC-002**: 100% of slot identity edge cases above are covered by current tests or identified as test gaps before slot behavior changes are planned.
- **SC-003**: No baseline requirement permits clients to create arbitrary start/end booking times.
- **SC-004**: No baseline requirement requires persisted slot rows for normal slot discovery.

## Assumptions

- This is a baseline specification for existing implemented behavior.
- Schedule administration is business-scoped and requires active owner or staff membership.
- External calendar synchronization is out of scope.
- Timezone ownership belongs to the business context and is consumed by timeslot workflows.
- Request-time active-state validation beyond explicit active business lookups remains deferred.

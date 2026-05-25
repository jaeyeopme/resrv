# Product Requirements

## Overview

`resrv` is a B2B reservation backend for businesses that need account-based access control,
resource scheduling, virtual slot discovery, and reservation lifecycle management.

The current redesign focuses on a timeslot booking product. A business configures booking settings,
creates resources, defines schedules, exposes virtual slots, and lets accounts hold and confirm
reservations.

## Users

| User | Need |
|---|---|
| Business owner | Create a business, own settings, manage staff access later |
| Business staff | Manage resources, schedules, and operational reservation transitions |
| Customer account | Find slots, hold a slot, confirm, release, or cancel own reservation |
| API reviewer/integrator | Inspect generated API docs and understand auth boundaries |

## Goals

- Model platform identity separately from booking behavior.
- Use `Account`, `Business`, and `BusinessMembership` as canonical product terms.
- Generate bookable slots from schedules and settings instead of persisting slot rows.
- Prevent active overbooking for the same business, resource, and time range.
- Keep expired holds from blocking capacity without requiring a cleanup worker.
- Expose generated Swagger/OpenAPI docs for review.
- Keep architecture decisions explicit in ADRs before merging the redesign.

## Non-Goals

- Payments, deposits, invoices, and refunds.
- Email, SMS, push notifications, or reminder delivery.
- Staff invitation and membership administration UI.
- Full customer profile management separate from platform `Account`.
- Distributed microservice deployment.
- External calendar sync.
- Login rate limiting and failed-login lockout in the current phase.

## Product Concepts

| Concept | Meaning |
|---|---|
| Account | Platform identity used by owners, staff, and customers |
| Business | Organization that owns booking settings, resources, schedules, and reservations |
| BusinessMembership | `OWNER` or `STAFF` access from an account to a business |
| Booking settings | Default slot duration, hold TTL, cancellation window, and max advance booking days |
| Resource | Reservable item such as a room, seat, equipment, or staff member |
| Schedule | Weekly windows plus optional date-specific override windows |
| Slot | Virtual bookable time range encoded as an opaque `slotId` |
| Reservation | Timestamp-fact record for hold, confirm, release, cancel, check-in, and no-show |

## Core Flows

### Platform Onboarding

1. Register account.
2. Login and receive account-scoped JWT.
3. Create business.
4. Owner membership is created for the business.

### Business Setup

1. Configure business booking settings.
2. Create resources.
3. Replace weekly schedule windows.
4. Replace date override windows when needed.

### Customer Booking

1. List active resources.
2. List virtual slots for a resource and date.
3. Hold a slot using `resourceId` and opaque `slotId`.
4. Confirm or release the hold.
5. Cancel confirmed reservation before the cancellation cutoff.

### Business Operations

1. Business owner or staff cancels a held or confirmed reservation.
2. Business owner or staff checks in a confirmed reservation after start time.
3. Business owner or staff marks no-show after end time.

## Acceptance Criteria

- Account login issues a JWT with account identity only.
- Business authorization is resolved server-side from active `BusinessMembership`.
- Timeslot booking must not trust client-supplied business role claims.
- Slot IDs must bind to business, resource, start time, and end time.
- Hold creation must reject stale, malformed, wrong-business, wrong-resource, or unavailable slots.
- Active blockers must include confirmed reservations and unexpired holds.
- Expired holds must stop blocking without a status mutation.
- Customer reservation transitions must require reservation ownership.
- Business reservation transitions must require active owner/staff membership.

## Open Product Questions

- Whether staff membership management belongs in the next platform milestone.
- Whether customers need profile data beyond `Account`.
- Whether business-facing reservation search/list endpoints are required before merge.
- Whether local review should run one combined API or separate platform/timeslot runtimes.


# Product requirements

## Overview

`resrv` is a backend for business reservation workflows that need account-based access control,
resource scheduling, virtual slot discovery, reservation lifecycle management, selected-seat ticket
purchase confirmation, and business activity review.

The requirements describe implemented backend behavior and current boundaries, not a production
rollout plan or commercial roadmap.

The current product scope covers timeslot booking and selected-seat ticket purchases. A business
configures booking settings, creates resources, defines schedules, exposes virtual slots, and lets
accounts hold and confirm reservations. Ticketing models event-owned inventory and selected seats,
lets authenticated customers confirm purchases for available seats, and lets authorized business
actors review completed purchase activity.

## Users

| User | Need |
|---|---|
| Business owner | Create a business, own settings, manage staff access, and review business activity |
| Business staff | Manage resources, schedules, operational reservation transitions, and business activity |
| Customer account | Find slots, manage own reservations, confirm selected-seat ticket purchases, and view own ticket history |
| API integrator | Inspect generated API docs and understand auth boundaries |

## Goals

- Model platform identity separately from booking behavior.
- Use `Account`, `Business`, and `BusinessMembership` as canonical product terms.
- Generate bookable slots from schedules and settings instead of persisting slot rows.
- Prevent active overbooking for the same business, resource, and time range.
- Keep expired holds from blocking capacity without requiring a cleanup worker.
- Require account recovery through password reset after repeated failed password sign-ins.
- Stop protected actions when account, business, or membership state becomes inactive.
- Let owners grant, review, update, disable, and audit staff membership without sharing credentials.
- Model selected-seat ticket purchases as first-successful ownership without overselling seats.
- Make repeated ticket purchase confirmation safe through customer-scoped idempotency replay.
- Expose generated Swagger/OpenAPI docs for inspection.
- Keep durable architecture decisions explicit in ADRs.

## Non-goals

- Payments, deposits, invoices, and refunds.
- SMS, push notifications, or reminder delivery.
- Staff invitation email delivery, acceptance workflow, and membership administration UI.
- Full customer profile management separate from platform `Account`.
- Distributed microservice deployment, message brokers, outbox processing, and event projections.
- External calendar sync.
- Ticket checkout attempts, ticket holds, failed-attempt ledgers, refunds, cancellations, resale,
  waitlists, seating-map UI, and public ticket marketing discovery.

## Product concepts

| Concept | Meaning |
|---|---|
| Account | Platform identity used by owners, staff, and customers |
| Business | Organization that owns booking settings, resources, schedules, reservations, and ticket events |
| BusinessMembership | `OWNER` or `STAFF` access from an account to a business |
| Booking settings | Default slot duration, hold TTL, cancellation window, and max advance booking days |
| Resource | Reservable item such as a room, seat, equipment, or staff member |
| Schedule | Weekly windows plus optional date-specific override windows |
| Slot | Virtual bookable time range encoded as an opaque `slotId` |
| Reservation | Timestamp-fact record for hold, confirm, release, cancel, check-in, and no-show |
| Ticket event | Ticketing-owned sale opportunity associated with a platform business |
| Ticket seat | Event-owned selected seat that can be available or purchased |
| Ticket purchase | Durable successful ownership of one or more selected seats by a customer account |
| Purchase idempotency key | Customer-scoped retry key that replays the original ticket purchase outcome |
| Sign-in protection | Account-scoped state requiring password reset after repeated failed password sign-ins |
| Password reset challenge | Single-use email recovery link that clears sign-in protection when completed |

## Core flows

### Platform onboarding

1. Register account.
2. Login and receive account-scoped JWT.
3. Create business.
4. Owner membership is created for the business.

### Business setup

1. Configure business booking settings.
2. Create resources.
3. Replace weekly schedule windows.
4. Replace date override windows when needed.

### Staff membership administration

1. Owner grants staff access to an existing active account by login email.
2. Owner lists current active and inactive memberships with account summaries.
3. Owner lists immutable access-change audit history.
4. Owner changes a membership role between `STAFF` and `OWNER`.
5. Owner disables membership; request-time business access uses the updated server-side state.

### Customer booking

1. List active resources.
2. List virtual slots for a resource and date.
3. Hold a slot using `resourceId` and opaque `slotId`.
4. Confirm or release the hold.
5. Cancel confirmed reservation before the cancellation cutoff.

### Customer ticket purchase

1. Confirm selected seats for an active ticket event with a customer-scoped idempotency key.
2. Receive the original public outcome again when retrying the same key and selected seats within
   the replay window.
3. Receive a stable unavailable-seat conflict when selected seats are already purchased.
4. View completed successful ticket purchases in customer ticket history.

### Business operations

1. Business owner or staff lists reservations for a business-local date.
2. Business owner or staff filters the list by resource, customer account, or derived state.
3. Business owner or staff cancels a held or confirmed reservation.
4. Business owner or staff checks in a confirmed reservation after start time.
5. Business owner or staff marks no-show after end time.
6. Business owner or staff reviews completed ticket purchase activity for an event they can access.

### Account recovery

1. Password sign-in failures remain non-enumerating.
2. The fifth failed password sign-in attempt for an existing account sends a password reset email.
3. Password sign-in for that account stays blocked until reset succeeds.
4. Password reset consumes the emailed token, updates the password hash, and clears sign-in
   protection.
5. Account-level sign-in protection does not affect other accounts or public booking availability.

## Acceptance criteria

- Account login issues a JWT with account identity only.
- Five failed password sign-in attempts for the same account require password reset through email.
- Password reset tokens must not be stored or logged as raw secrets.
- Protected authenticated actions must reject inactive accounts at request time.
- Business authorization is resolved server-side from active `BusinessMembership`.
- Timeslot booking must not trust client-supplied business role claims.
- Business-scoped owner/staff actions must require active account, active business, and active
  owner/staff membership.
- Slot IDs must bind to business, resource, start time, and end time.
- Hold creation must reject stale, malformed, wrong-business, wrong-resource, or unavailable slots.
- Active blockers must include unexpired holds, confirmed reservations, and checked-in reservations.
- Expired holds must stop blocking without a status mutation.
- Customer reservation transitions must require reservation ownership.
- Business reservation transitions must require active owner/staff membership.
- Business reservation list/search must require active owner/staff membership.
- Staff membership administration must require active owner membership and must preserve at least
  one active owner per business.
- Membership grant, role change, reactivation, and disablement must append access audit entries.
- Public booking discovery must remain reachable while excluding inactive businesses and inactive
  resources from bookable results.
- Ticket purchase confirmation must require an authenticated customer account and a non-empty
  selected-seat request.
- Ticket purchase confirmation must require a customer-scoped idempotency key.
- Ticket purchase confirmation must create all requested selected-seat ownership or none.
- Contending selected-seat purchase confirmations must not oversell seats or create partial
  purchases.
- Ticket purchase idempotency must replay the original public outcome for the configured replay
  window and reject changed or expired same-key retries with stable public reasons.
- Customer ticket history must include only completed successful purchases for the authenticated
  account.
- Business ticket activity must require active owner/staff membership and must not reveal whether an
  inaccessible ticket event exists.

## Open product questions

- Whether customers need profile data beyond `Account`.
- Current runtime packaging uses the platform runtime to serve platform, booking, and
  ticketing APIs together. `timeslot` and `ticketing` local `bootRun` tasks remain disabled until a
  separate runtime-split and outbox/message-broker design is planned.
- Whether password reset needs a first-party web screen in this repository or an external client
  route.

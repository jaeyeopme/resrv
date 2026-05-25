# Product

`resrv` is a multi-tenant B2B reservation API. It is currently focused on a timeslot-booking
model: one business owns resources, resources expose schedules, customers select virtual slots,
and reservations move through hold/confirm/operations transitions.

## Users

| User | Meaning |
|---|---|
| Account | Platform identity used for owners, staff, and customers |
| Business owner/staff | Account with active `BusinessMembership` role `OWNER` or `STAFF` |
| Customer | Account that holds and confirms a reservation |
| Integrator | External client using the REST API |

Customer is now a platform `Account`. Business replaces Tenant in domain and API terminology.
`BusinessMembership` grants `OWNER` or `STAFF` access to a Business.

## Booking Concepts

| Term | Meaning |
|---|---|
| Business | Organization that owns booking configuration |
| Resource | Reservable item such as room, seat, equipment, or staff member |
| Booking settings | Slot duration, hold TTL, cancellation window, max advance booking days |
| Schedule | Weekly windows plus optional date override windows |
| Slot | Virtual bookable time range generated from schedule and settings |
| Reservation | Timestamp-fact record for hold, confirm, cancel, check-in, and no-show |

Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
Slots are virtual and selected by opaque `slotId`.

## Reservation Policy

- One resource/time slot can have at most one active blocker.
- A hold expires by time comparison, not by mutating a persisted status.
- Reservation state is derived from timestamp facts; `HELD` and `EXPIRED` are not persisted statuses.
- Expired hold cleanup worker is not part of correctness.
- UTC instants are stored; business timezone is used for schedule generation and API time display.

## Current Boundary

The current system targets RESTful backend quality, not ticketing-scale infrastructure. The design
keeps scale-out correctness in the hot hold path through DB transaction locking and active blocker
queries while avoiding scheduler-dependent correctness.

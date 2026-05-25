# Current Status and Future Work

## Current Milestone

The tenant-local API has been replaced by a platform + timeslot booking split.

Implemented deployables:

- `platform-api`
- `timeslot-booking-api`

## Implemented Foundation

- Platform `Account` registration/login
- `Business` creation
- `BusinessMembership` owner/staff access model
- Timeslot booking settings
- Reservable resources
- Weekly schedules and date overrides
- Virtual slot generation with opaque `slotId`
- Reservation hold, confirm, release, cancel, check-in, and no-show flows
- PostgreSQL advisory transaction lock for hold hot path
- Active blocker query for confirmed reservations and unexpired holds
- Reservation state derived from timestamp facts
- Spring Security JWT resource server per deployable
- Testcontainers-backed API and persistence tests

Customer is now a platform `Account`. Business replaces Tenant in domain and API terminology.
`BusinessMembership` grants `OWNER` or `STAFF` access to a Business.

Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
Slots are virtual and selected by opaque `slotId`.

Reservation state is derived from timestamp facts; `HELD` and `EXPIRED` are not persisted statuses.
Expired hold cleanup worker is not part of correctness.

## Deferred

| Item | Reason |
|---|---|
| Login rate limiting | Operations policy and storage choice needed |
| Failed-login lockout | Requires user-state and unlock policy |
| Active hold quota per customer | Abuse hardening, separate from slot correctness |
| Payments/deposits | Product integration outside current backend boundary |
| Notification outbox | Operational expansion |

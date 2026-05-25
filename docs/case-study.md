# Case Study — Reservation rules to verified backend behavior

This is a short technical narrative. It does not replace the API or architecture docs.

## 1. Problem

A reservation backend must coordinate identity, business authorization, resource schedules,
timezones, concurrent holds, and reservation transitions.

The hard part is not creating rows. The hard part is keeping account identity, business access,
time-derived hold expiry, and data constraints consistent across the whole flow.

## 2. Design Choices

| Concern | Choice | Why it matters |
|---|---|---|
| Identity | Customer is a platform `Account` | One identity model works for owners, staff, and customers |
| Business boundary | Business replaces Tenant terminology | API and domain vocabulary match B2B product language |
| Authorization | `BusinessMembership` grants `OWNER`/`STAFF` access | JWTs stay account-scoped; access stays server-side |
| API shape | `platform-api` and `timeslot-booking-api` | Platform lifecycle and booking traffic can scale separately |
| Slots | Virtual slots selected by opaque `slotId` | No persisted slot table needed for regular schedules |
| Hold correctness | Advisory transaction lock plus active blocker query | Hot-slot races are serialized at the database transaction boundary |
| State model | Reservation state derived from timestamp facts | `HELD` and `EXPIRED` are not persisted statuses |

See [`decisions.md`](decisions.md) for the durable decision summary.

## 3. Implementation Evidence

| Behavior | Where to inspect |
|---|---|
| Account login and business creation | `platform-api`, `platform-adapter-web`, `platform-application` |
| Business membership persistence | `platform-adapter-persistence` |
| Booking settings/resources/schedules | `timeslot-application`, `timeslot-adapter-persistence`, `timeslot-adapter-web` |
| Virtual slot generation | `timeslot-application/src/main/java/io/resrv/timeslot/application/slot` |
| Reservation facts and transitions | `timeslot-domain/src/main/java/io/resrv/timeslot/domain/reservation` |
| Hold lock and active blocker query | `timeslot-adapter-persistence` reservation and lock adapters |
| End-to-end booking API flow | `timeslot-booking-api/src/test/java/io/resrv/timeslot/api/TimeslotBookingApiIntegrationTest.java` |

## 4. What This Demonstrates

`resrv` demonstrates a backend feature carried from product rules through API design, security
boundaries, persistence constraints, and automated verification. The value is not the endpoint
count, but the way the endpoints, business model, reservation rules, and tests reinforce each
other.

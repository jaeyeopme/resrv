# Architecture

`resrv` is a Java 25 + Spring Boot 4 backend organized around bounded contexts and
hexagonal boundaries.

## Deployables

| Deployable | Role |
|---|---|
| `platform-api` | Account login, Business creation, BusinessMembership ownership |
| `timeslot-booking-api` | Resource schedules, virtual slots, hold/confirm/reservation transitions |

## Modules

| Module | Responsibility |
|---|---|
| `shared-kernel` | Shared IDs and timezone value objects |
| `platform-domain` | Account, Business, BusinessMembership domain |
| `platform-application` | Platform use cases and ports |
| `platform-adapter-persistence` | Platform JPA/Flyway persistence |
| `platform-adapter-web` | Platform REST adapters |
| `platform-api` | Platform runtime assembly/security |
| `timeslot-domain` | Booking settings, resources, schedules, slots, reservation facts |
| `timeslot-application` | Timeslot use cases and ports |
| `timeslot-adapter-persistence` | Timeslot JPA/Flyway persistence and slot advisory lock |
| `timeslot-adapter-web` | Timeslot REST adapters |
| `timeslot-booking-api` | Timeslot runtime assembly/security and platform read adapter |

## Domain Direction

Domain modules do not depend on Spring, JPA, adapters, or API runtimes. Application modules
define ports. Adapters implement ports. Runtime modules assemble security, persistence, and web
adapters.

Business replaces Tenant in domain and API terminology. Customer is now a platform `Account`.
`BusinessMembership` grants `OWNER` or `STAFF` access to a Business.

## Reservation Correctness

Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
Slots are virtual and selected by opaque `slotId`.

Hold creation uses a PostgreSQL advisory transaction lock for the selected resource/start time,
then queries active blockers. Active blockers are confirmed reservations or holds whose
`holdExpiresAt` is still in the future.

Reservation state is derived from timestamp facts; `HELD` and `EXPIRED` are not persisted
statuses. Expired hold cleanup worker is not part of correctness.

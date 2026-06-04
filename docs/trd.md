# Technical Requirements And Design

## Scope

This document describes the current implementation. ADRs are the source of truth for decisions; this
TRD explains how those decisions appear in the codebase. It is not a deployment guide.

Key decision areas are:

- Identity and access: account-scoped JWTs, business membership boundaries, and owner/staff
  administration.
- Runtime and modules: bounded-context Gradle modules, one platform runtime, and pure Java
  `platform-exchange` APIs.
- Booking correctness: virtual slots, derived reservation state, and advisory-lock-backed hold
  persistence.
- Ticketing correctness: selected-seat purchase ownership, concurrency-safe seat claims, and
  idempotency-key replay.
- API contract: generated OpenAPI from the platform runtime.

See the [ADR index](adr/README.md) for the full decision index.

## Runtime And Build

| Area | Current design |
|---|---|
| Runtime | Spring Boot 4, Spring MVC, Spring Security |
| Language | Java 25 |
| Build | Gradle 9 multi-module build |
| Database | PostgreSQL 16 |
| Migrations | Flyway migrations in bounded-context modules |
| API docs | Springdoc generated OpenAPI and Swagger UI |
| Operational probes | Actuator health, liveness, and readiness |
| Container packaging | Jib image for the platform runtime |
| Tests | JUnit 5, Spring Boot tests, Testcontainers, ArchUnit |

The current contention strategy uses PostgreSQL as the coordination point for scarce capacity. The
correctness mechanisms are database-backed rather than JVM-local: reservation holds, selected-seat
claims, and purchase idempotency do not depend on single-process memory. Throughput scaling for
sold-out-event traffic is a separate design problem.

## Current Module Baseline

The backend has 5 Gradle modules:

| Module | Responsibility |
|---|---|
| `shared-kernel` | Shared IDs and timezone value object |
| `platform-exchange` | Pure Java platform-owned exchange APIs for cross-context lookup/check decisions |
| `platform` | Platform domain, use cases, adapters, Flyway migration, canonical Spring Boot runtime, security, and Jib packaging |
| `ticketing` | Ticketing domain, use cases, adapters, Flyway migration, and platform exchange adapter contributed to the platform runtime |
| `timeslot` | Timeslot domain, use cases, adapters, Flyway migration, booking API assembly, and platform exchange adapter contributed to the platform runtime |

Hexagonal layers remain as Java package boundaries inside `platform`, `ticketing`, and `timeslot`. ArchUnit
enforces dependency direction, keeps direct database access in outbound adapter packages, and limits
timeslot/ticketing-to-platform dependencies to the explicit `platform-exchange` APIs consumed by
outbound platform adapters.

## API Boundary

Platform API owns account and business lifecycle:

- Register account.
- Login.
- Complete password reset from an emailed reset token.
- Create business and owner membership.
- Grant staff membership to an existing active account.
- List current business memberships.
- List membership audit history.
- Update membership role by membership id.
- Disable membership by membership id.

Timeslot API owns booking lifecycle:

- Upsert booking settings.
- Create/list resources.
- Replace weekly schedules and date overrides.
- List virtual slots.
- Hold, confirm, release, cancel, check-in, and mark no-show reservations.
- List business reservations for a business-local date, with optional resource, customer account,
  and derived-state filters.

Ticketing owns selected-seat purchase lifecycle behavior:

- Ticket sale events tied to platform business ids.
- Event occurrence windows and sale windows.
- Tiered inventory totals, reserved counts, confirmed counts, soft-reserved counts, and derived
  available counts.
- Event-owned selected seats with available or purchased state.
- Customer ticket purchases that own one or more selected seats.
- Customer ticket history for authenticated customers.
- Business purchase activity for authorized owner/staff actors.

`platform` is the canonical backend runtime. It scans platform, timeslot, and ticketing
bounded-context packages, serves platform, booking, and ticketing API groups, and exposes one
generated OpenAPI surface from `/v3/api-docs`. Ticketing contributes purchase confirmation,
customer history, and business purchase activity endpoints through platform web adapters.

Generated OpenAPI from that runtime is the API contract surface. Narrative docs describe API groups,
authorization boundaries, and design decisions, but do not maintain a duplicate endpoint catalog.

Timeslot resource APIs use resource UUIDs as the only resource identity. Resource create and replace
requests do not accept slug, handle, or URL-safe resource key fields; obsolete identity fields are
invalid request fields. Resource responses, public resource discovery, and customer reservation
resource summaries omit resource slug/handle fields. Duplicate resource names are allowed.

`timeslot` keeps an application class for module-local testing history, but its `bootJar` and
`bootRun` tasks remain disabled. `ticketing` is also a non-executable module. Booking APIs and
ticketing beans are served by the platform runtime. ADR-0022 and ADR-0023 do not add separate
timeslot or ticketing runtimes, service-to-service transport, message broker, outbox, events, or
projections.

The platform runtime exposes liveness and readiness health probes. Readiness includes database
availability and should be checked before routing requests to the backend. Probe responses expose
status only and must not expose secrets or private account, business, or reservation data.

## Security Design

JWTs are account-scoped:

- `sub`: account UUID.
- `accountId`: account UUID.
- `jti`: token identifier.
- `iss`: configured issuer.
- `aud`: configured audience.
- `exp`: expiration.

JWTs do not carry `businessId` or business role claims. Business access is resolved through active
membership data.

Repeated failed password sign-ins are tracked as platform account security state. On the fifth
failed password attempt for an existing account, platform creates a password reset challenge, sends a
password reset email, and blocks password sign-in until reset succeeds. Reset tokens are stored only
as digests.

Protected platform requests pass through an active-account check after JWT authentication.
Business-scoped owner/staff authorization requires active account, active business, and active
membership through `BusinessAccessCheck`. Timeslot and ticketing obtain those decisions through
explicit `platform-exchange` APIs and must not read platform tables directly.

Membership administration is stricter than generic business access: grant, list, audit, role update,
and disable operations require active owner membership. JWTs still carry only account identity.

IDOR-sensitive object probes use uniform public not-found responses. Customer reservation detail,
confirm, release, and customer-cancel return the same `404` status and detail for missing and
not-owned reservations. Resource-scoped mutations return a generic resource not-found response for
missing or wrong-business resource ids. Internal diagnostic facts may be logged, but they are not
returned in problem details.

## Persistence Design

Platform schema:

- `platform.account`
- `platform.business`
- `platform.business_membership`
- `platform.business_membership_audit_entry`
- `platform.sign_in_attempt`
- `platform.account_sign_in_protection`
- `platform.password_reset_challenge`

Timeslot schema:

- `timeslot.business_booking_settings`
- `timeslot.resource`
- `timeslot.resource_weekly_schedule`
- `timeslot.resource_weekly_schedule_window`
- `timeslot.resource_date_schedule_override`
- `timeslot.resource_date_schedule_override_window`
- `timeslot.reservation`

Timeslot records reference `business_id` and `customer_account_id` by UUID. The current migrations
do not add cross-schema foreign keys from timeslot to platform.

`timeslot.resource` no longer stores resource slug data or business-scoped resource slug
uniqueness. Reservations remain linked through `resource_id`, so resource display-detail changes and
slug removal do not rewrite reservation rows.

Ticketing schema:

- `ticketing.ticket_event`
- `ticketing.ticket_inventory`
- `ticketing.ticket_inventory_tier`
- `ticketing.ticket_seat`
- `ticketing.ticket_purchase`
- `ticketing.ticket_purchase_seat`
- `ticketing.ticket_purchase_idempotency`

Ticketing records reference platform `business_id` by UUID and ticketing-owned ids by UUID. The
current migration does not add cross-schema foreign keys from ticketing to platform.

Production persistence code defaults to Spring Data JPA for owned tables. Native SQL or JDBC is
reserved for outbound adapters where database-specific behavior is required, such as PostgreSQL
advisory locks or compact read projections. Timeslot and ticketing obtain platform business and
membership data through platform application exchange APIs rather than reading platform tables
directly.

## Ticket Purchase Correctness

Selected-seat purchase confirmation validates the authenticated customer, active event, non-empty
seat selection, duplicate seat ids, event ownership for each seat, current seat availability, and a
required customer-scoped `idempotencyKey`. All selected seats are purchased together or none are.
The first successful confirmation creates one `ticket_purchase` row and marks selected
`ticket_seat` rows as `PURCHASED`.

The contention-sensitive claim is executed in a ticketing outbound persistence adapter with
database row coordination and deterministic seat ordering. Losing concurrent confirmations return
an unavailable-seats outcome as `409 Conflict` without creating a purchase, exposing another
customer, or leaving partial seat ownership.

Idempotency records bind the authenticated customer, idempotency key, event, and selected seat set.
The same key and request replay the original purchased or unavailable public outcome for 24 hours.
The same key with different purchase details is rejected as an invalid retry during that window.
After the replay window, retained records reject reuse as an expired key. Cleanup eligibility is
stored as a timestamp 30 days after replay expiry; purchase correctness does not depend on a cleanup
job running.

The ticketing web boundary maps idempotency problems to stable public reasons `invalid_retry` and
`expired_key`. Generated OpenAPI documents these public values; internal enum names are not part of
the API contract.

Ticketing deliberately does not store checkout attempts, general failed attempts, cancellations, or
expirations. The idempotency table is the minimal customer-scoped replay record, not a general
failed-attempt ledger. Business purchase activity uses current server-side business access checks
and keeps unauthorized or missing event probes non-enumerating.

## Reservation Correctness

Hold creation:

1. Decode and validate `slotId`.
2. Load active business, settings, resource, and schedule.
3. Acquire advisory transaction lock for `resourceId|slotStartAt`.
4. Query active blockers.
5. Save the hold when no blocker exists.

Active blocker query includes:

- Confirmed reservations.
- Checked-in reservations.
- Holds where `hold_expires_at > now`.

It excludes released, cancelled, and no-show reservations.

## Reservation State

Reservation state is derived from facts:

- `confirmed_at`
- `released_at`
- `cancelled_at`
- `cancelled_by`
- `checked_in_at`
- `no_show_at`
- `hold_expires_at`

`HELD` and `EXPIRED` are not persisted statuses.

## Time Handling

- Persistence stores UTC instants.
- Business timezone is used to generate schedules and virtual slots.
- Slot and reservation responses are rendered in the business timezone when the business can be
  resolved.

## Configuration

Required JWT properties:

```text
resrv.jwt.secret-key
resrv.jwt.issuer
resrv.jwt.audience
resrv.jwt.expiration
```

Spring Boot relaxed binding allows equivalent environment variables:

```text
RESRV_JWT_SECRET_KEY
RESRV_JWT_ISSUER
RESRV_JWT_AUDIENCE
RESRV_JWT_EXPIRATION
```

Database configuration can come from Spring Boot Docker Compose support in local development or
standard Spring datasource properties in other environments. The platform runtime loads
`classpath:db/migration`, which includes platform migrations from the platform module plus timeslot
and ticketing migrations from module runtime classpaths.

The `prod` profile disables local Docker Compose discovery and expects explicit datasource, JWT, and
password reset settings from the environment.

Password reset email configuration uses Spring Mail plus feature properties:

```text
spring.mail.*
resrv.security.password-reset.public-base-url
resrv.security.password-reset.token-ttl
```

## Open Technical Decisions

- Decide any future runtime split strategy for domains that outgrow the current modular monolith,
  including process boundaries, transport, outbox/message broker, event schemas, replay/backfill,
  and projection repair.
- Decide whether password reset link handling is owned by this repository or by a separate client.
- Decide the production SMTP provider, sender identity, and delivery failure policy.

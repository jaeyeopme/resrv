# Technical Requirements And Design

## Scope

This document describes the current redesign branch implementation. ADRs are the source of truth for
decisions; this TRD explains how those decisions appear in the codebase.

Primary ADRs:

- [ADR-0001](adr/0001-bounded-context-module-baseline.md): superseded 11-module baseline.
- [ADR-0017](adr/0017-collapse-to-bounded-context-modules.md): active bounded-context module
  decision.
- [ADR-0003](adr/0003-platform-account-identity.md): platform account identity.
- [ADR-0004](adr/0004-business-and-membership-boundary.md): business membership boundary.
- [ADR-0006](adr/0006-account-scoped-jwt.md): account-scoped JWTs.
- [ADR-0010](adr/0010-virtual-slots.md): virtual slots.
- [ADR-0011](adr/0011-derived-reservation-state.md): derived reservation state.
- [ADR-0012](adr/0012-reservation-persistence-and-locking.md): hold correctness.
- [ADR-0014](adr/0014-timeslot-booking-api-boundary.md): timeslot API boundary.
- [ADR-0015](adr/0015-replace-tenant-booking-api.md): replacement of the old tenant API.

## Runtime And Build

| Area | Current design |
|---|---|
| Runtime | Spring Boot 4, Spring MVC, Spring Security |
| Language | Java 25 |
| Build | Gradle 9 multi-module build |
| Database | PostgreSQL 16 |
| Migrations | Flyway migrations in bounded-context modules |
| API docs | Springdoc generated OpenAPI and Swagger UI |
| Tests | JUnit 5, Spring Boot tests, Testcontainers, ArchUnit |

## Current Module Baseline

The current branch has 3 Gradle modules:

| Module | Responsibility |
|---|---|
| `shared-kernel` | Shared IDs and timezone value object |
| `platform` | Platform domain, use cases, adapters, Flyway migration, Spring Boot runtime, and security |
| `timeslot` | Timeslot domain, use cases, adapters, Flyway migration, booking API assembly, and platform contract adapter |

Hexagonal layers remain as Java package boundaries inside `platform` and `timeslot`. ArchUnit
enforces dependency direction, keeps direct database access in outbound adapter packages, and limits
timeslot-to-platform dependencies to explicit `platform.contract` types.

## API Boundary

Platform API owns account and business lifecycle:

- Register account.
- Login.
- Create business and owner membership.

Timeslot API owns booking lifecycle:

- Upsert booking settings.
- Create/list resources.
- Replace weekly schedules and date overrides.
- List virtual slots.
- Hold, confirm, release, cancel, check-in, and mark no-show reservations.
- List business reservations for a business-local date, with optional resource, customer account,
  and derived-state filters.

`timeslot` has an application class, but local runtime packaging is not final because `bootJar` and
`bootRun` are currently disabled.

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

## Persistence Design

Platform schema:

- `platform.account`
- `platform.business`
- `platform.business_membership`

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

Production persistence code defaults to Spring Data JPA for owned tables. Native SQL or JDBC is
reserved for outbound adapters where database-specific behavior is required, such as PostgreSQL
advisory locks. Timeslot obtains platform business and membership data through platform application
contracts rather than reading platform tables directly.

## Reservation Correctness

Hold creation:

1. Decode and validate `slotId`.
2. Load active business, settings, resource, and schedule.
3. Acquire advisory transaction lock for `resourceId|slotStartAt`.
4. Query active blockers.
5. Save the hold when no blocker exists.

Active blocker query includes:

- Confirmed reservations.
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
standard Spring datasource properties in other environments.

## Open Technical Decisions

- Decide whether `timeslot` should produce a bootable artifact now.
- Decide whether platform and timeslot should run as separate local processes or a combined review
  runtime while the product remains monorepo-only.

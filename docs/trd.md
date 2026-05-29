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
- [ADR-0018](adr/0018-account-security-hardening.md): password reset recovery and active-state
  checks.
- [ADR-0020](adr/0020-platform-exchange-boundary.md): pure Java platform exchange module for
  cross-context lookup/check APIs.
- [ADR-0021](adr/0021-staff-membership-administration.md): owner-only staff membership
  administration with append-only audit.

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

The current branch has 4 Gradle modules:

| Module | Responsibility |
|---|---|
| `shared-kernel` | Shared IDs and timezone value object |
| `platform-exchange` | Pure Java platform-owned exchange APIs for cross-context lookup/check decisions |
| `platform` | Platform domain, use cases, adapters, Flyway migration, Spring Boot runtime, and security |
| `timeslot` | Timeslot domain, use cases, adapters, Flyway migration, booking API assembly, and platform exchange adapter |

Hexagonal layers remain as Java package boundaries inside `platform` and `timeslot`. ArchUnit
enforces dependency direction, keeps direct database access in outbound adapter packages, and limits
timeslot-to-platform dependencies to the explicit `platform-exchange` APIs consumed by the timeslot
outbound platform adapter.

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

`timeslot` has an application class, but local runtime packaging is not final because `bootJar` and
`bootRun` are currently disabled. Spec 009 establishes only the compile-time exchange boundary; it
does not add a separate timeslot runtime, service-to-service transport, message broker, outbox,
events, or projections.

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
membership through `BusinessAccessCheck`. Timeslot obtains those decisions through explicit
`platform-exchange` APIs and must not read platform tables directly.

Membership administration is stricter than generic business access: grant, list, audit, role update,
and disable operations require active owner membership. JWTs still carry only account identity.

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

Production persistence code defaults to Spring Data JPA for owned tables. Native SQL or JDBC is
reserved for outbound adapters where database-specific behavior is required, such as PostgreSQL
advisory locks. Timeslot obtains platform business and membership data through platform application
exchange APIs rather than reading platform tables directly.

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
standard Spring datasource properties in other environments.

Password reset email configuration uses Spring Mail plus feature properties:

```text
spring.mail.*
resrv.security.password-reset.public-base-url
resrv.security.password-reset.token-ttl
```

## Open Technical Decisions

- Decide the future runtime split strategy for traffic-sensitive domains such as timeslot and
  ticketing, including process boundaries, transport, outbox/message broker, event schemas,
  replay/backfill, and projection repair.
- Decide whether password reset link handling is owned by this repository or by a separate client.
- Decide the production SMTP provider, sender identity, and delivery failure policy.

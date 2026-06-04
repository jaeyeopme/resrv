# Testing Strategy

This document explains the verification strategy for this backend. It focuses on runnable checks and
the behavior those checks protect.

## Goals

- Verify domain invariants without Spring.
- Verify application use cases through ports.
- Verify persistence mappings and database constraints with PostgreSQL.
- Verify API/runtime wiring with Spring Boot integration tests.
- Enforce architecture boundaries with ArchUnit.
- Keep line coverage gates visible per module.

## Commands

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

Docker must be running because persistence and API integration tests use Testcontainers.

## Test Layers

| Layer | Location | Purpose |
|---|---|---|
| Shared kernel tests | `shared-kernel/src/test` | ID and timezone primitives |
| Domain tests | `platform/src/test`, `timeslot/src/test`, `ticketing/src/test` | Entity/value object invariants |
| Application tests | `platform/src/test`, `timeslot/src/test`, `ticketing/src/test` | Use case behavior with fake ports |
| Persistence tests | `platform/src/test`, `timeslot/src/test`, `ticketing/src/test` | JPA mapping, Flyway schema, PostgreSQL behavior |
| API integration tests | `platform/src/test`, `timeslot/src/test` | Security, HTTP flow, runtime wiring, including ticketing endpoints assembled into `platform` |
| Architecture tests | `platform/src/test/.../architecture`, `timeslot/src/test/.../architecture`, `ticketing/src/test/.../architecture` | Package/module dependency rules |
| Platform exchange architecture tests | `platform-exchange/src/test` | Pure Java exchange boundary and event-package guard |

## Coverage Gates

JaCoCo line coverage minimums are configured in the root `build.gradle.kts`.

| Module group | Current threshold |
|---|---|
| `shared-kernel` | 85% |
| `platform` | 80% |
| `ticketing` | 80% |
| `timeslot` | 80% |

`check` depends on `jacocoTestCoverageVerification` for modules that apply JaCoCo.
`platform` and `timeslot` also keep package-level gates for application, web adapter, and
persistence adapter packages so aggregate module coverage cannot hide a drop in one layer.

## Architecture Rules

ArchUnit verifies:

- Platform domain does not depend on application, adapters, or API runtime.
- Platform domain has no Spring, Jakarta, or Hibernate dependencies.
- Platform application does not depend on adapters or API runtime.
- Only the platform API runtime layer may assemble timeslot classes.
- Timeslot domain does not depend on application, adapters, or API runtime.
- Timeslot domain has no Spring, Jakarta, or Hibernate dependencies.
- Timeslot application does not depend on adapters or API runtime.
- Timeslot does not depend on platform domain, adapters, API runtime, repositories, entities, or
  persistence schema.
- Only the timeslot outbound platform adapter may depend on explicit `platform-exchange` APIs.
- Ticketing does not depend on platform domain, adapters, API runtime, repositories, entities, or
  persistence schema.
- Only the ticketing outbound platform adapter may depend on explicit `platform-exchange` APIs.
- Ticketing selected-seat and purchase domain code stays inside the ticketing domain and has no
  Spring, Jakarta, Hibernate, or platform dependencies.
- Platform exchange APIs do not depend on Spring, Jakarta, Hibernate, platform implementation
  packages, or exchange event packages.
- Direct database access primitives stay inside outbound adapter packages in production code.
- Request-handling Web Adapter classes do not depend on endpoint-level Swagger/OpenAPI
  annotations; those annotations live on same-package `*ApiDocs` interfaces implemented by the
  adapters.

## Behavior Coverage

The test suite is organized around behavior guarantees rather than endpoint catalogs:

| Area | Protected behavior |
|---|---|
| Account security | Password reset protection after repeated failed sign-ins, fake email delivery in API tests, inactive-account denial, and public documentation reachability |
| Business access | Owner/staff membership checks, membership audit history, last-owner protection, and request-time access decisions |
| Resource lifecycle | Replacement semantics for settings, resources, schedules, date overrides, resource ID-only identity, and inactive-resource public discovery exclusion |
| Public booking | Business slug discovery, active resource and generated slot discovery, collapsed public not-found responses, and authenticated hold creation |
| Reservation contention | Generated-slot validation, timezone boundaries, advisory lock ordering, active blocker checks, expired-hold behavior, IDOR-safe responses, and contention outcomes |
| Runtime packaging | Platform runtime assembly of platform, booking, and ticketing API groups; migration loading; generated OpenAPI exposure; and unsupported runtime exclusion |
| Operational readiness | Public liveness/readiness probes, database-backed readiness, migration visibility, OpenAPI smoke reachability, and absence of secrets in health responses |
| API contract | Generated OpenAPI path/method coverage, representative success/failure documentation, and public/private schema boundaries |

Ticketing API tests run through the platform runtime because ticketing has no standalone backend
runtime. Focused ticketing verification is:

```bash
./gradlew :platform:test --tests '*Ticketing*'
./gradlew :platform:test --tests '*Concurrency*' --tests '*HighContention*'
```

Those tests cover generated OpenAPI for purchase confirmation, customer ticket history, and business
ticket activity; unavailable selected seats as `409 Conflict`; idempotency problem reasons
`invalid_retry` and `expired_key`; non-enumerating not-found responses for missing versus
unauthorized business activity probes; and concurrent selected-seat claim behavior. Run
`./gradlew :ticketing:test` only when ticketing application, domain, or persistence behavior
changes.

Ticket purchase idempotency uses a 24-hour replay window. Expired idempotency records remain retained
until 30 days after replay expiry and may be cleaned later, but cleanup is not required for purchase
correctness.

## High-Contention Correctness Review

Use the architecture high-contention correctness guidance before implementing a new flow that can
lose correctness when many callers compete for the same limited capacity. Review language must preserve
each bounded context's own terms: reservation work can talk about generated slots and active
blockers, while ticketing work can talk about selected-seat ownership and purchase idempotency.

Review questions:

- Can capacity be overbooked, oversold, or partially claimed under concurrent attempts?
- Does the flow protect generated availability and active blockers, selected ownership, or another
  explicit invariant?
- What repeat-request behavior is expected: replay, invalid retry, expired retry, or new attempt?
- Does expiry release correctness immediately, retain rejection behavior, or only permit cleanup?
- Which lifecycle states are terminal, reversible, expired, or conflict-producing?
- Which public responses must remain stable for losing contention or unauthorized probes?
- Does the feature need a fresh spec or ADR because it adds new product or runtime behavior?

Future public behavior changes for high-contention flows must be visible through the accepted public
contract and covered by end-to-end verification appropriate to the change. API behavior changes must
update generated OpenAPI coverage and API integration tests. Documentation-only pattern updates
should state why runtime tests were not run.

## Testcontainers

Integration and persistence tests use PostgreSQL through Testcontainers. Test properties configure:

```text
spring.datasource.url=jdbc:tc:postgresql:16:///resrv
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
```

JWT tests use a fixed local test secret and test issuer/audience values.

## Runtime Packaging Verification

```bash
./gradlew :platform:test --tests io.resrv.platform.api.PlatformRuntimePackagingIntegrationTest
./gradlew :platform:test --tests io.resrv.platform.api.PlatformOperationalReadinessIntegrationTest
./gradlew :platform:bootJar
./gradlew :platform:jibDockerBuild
```

## Known Gaps

- Separate timeslot or ticketing service runtimes are deferred until a later explicit runtime split
  and outbox/message-broker design.
- Token revocation for account-scoped JWTs is deferred.

# Testing Strategy

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
| Domain tests | `platform/src/test`, `timeslot/src/test` | Entity/value object invariants |
| Application tests | `platform/src/test`, `timeslot/src/test` | Use case behavior with fake ports |
| Persistence tests | `platform/src/test`, `timeslot/src/test` | JPA mapping, Flyway schema, PostgreSQL behavior |
| API integration tests | `platform/src/test`, `timeslot/src/test` | Security, HTTP flow, runtime wiring |
| Architecture tests | `platform/src/test/.../architecture`, `timeslot/src/test/.../architecture` | Package/module dependency rules |

## Coverage Gates

JaCoCo line coverage minimums are configured in the root `build.gradle.kts`.

| Module group | Current threshold |
|---|---|
| `shared-kernel` | 85% |
| `platform` | 80% |
| `timeslot` | 80% |

`check` depends on `jacocoTestCoverageVerification` for modules that apply JaCoCo.
The bounded-context modules also keep package-level gates for application, web adapter, and
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
- Direct database access primitives stay inside outbound adapter packages in production code.

Reservation list/search tests verify business membership authorization, business-local date
windows, optional resource/customer/state filters, and deterministic start-time ordering.

Customer reservation history tests verify self-scoped list/detail APIs, owner-only account
filtering, inactive business/resource summary rendering, bounded page/size validation, stable
descending ordering, derived state and `upcoming=true` filtering before pagination, and identical
public `404` responses for missing and not-owned detail, confirm, release, and customer-cancel
lookups.

Business resource lifecycle tests verify full replacement semantics for booking settings, resource
details, booking overrides, weekly schedules, and date overrides. They also cover explicit
activate/deactivate actions, public discovery exclusion for inactive resources, future-only policy
effects for holds and cancellation cutoffs, and reservation fact preservation after lifecycle
changes.

Resource probe tests compare missing and wrong-business resource identifiers at the API boundary so
public problem details do not expose resource ownership or existence facts.

Staff membership administration tests verify owner-only grant/list/audit/update/disable APIs,
duplicate active membership rejection, disabled membership reactivation, last-owner protection,
wrong-business membership denial, immutable audit entries, generated OpenAPI response documentation,
and request-time access decisions from current membership state.

Public booking discovery tests verify slug-based business discovery, active-only resource discovery,
schedule-derived slots with `available` state, malformed-input validation, collapsed `404` responses
for valid missing/inactive/not-bookable/wrong-business lookups, no public business UUID exposure,
and business-slug-scoped authenticated hold creation.

Platform runtime packaging tests verify that the canonical platform runtime serves booking settings
and public booking discovery endpoints, applies platform and timeslot schemas, rejects inactive
accounts for protected booking actions, preserves non-enumerating wrong-business public slot lookup
responses, exposes platform plus booking endpoint groups from generated OpenAPI, excludes
unsupported capability groups, verifies public/private schema boundaries, and checks that human docs
do not duplicate a hand-written endpoint catalog.

Operational readiness tests verify public liveness/readiness probes, database-backed readiness,
Flyway migration history visibility for platform and timeslot migrations, generated OpenAPI
reachability for smoke checks, and documentation drift around unsupported standalone services.

API contract consistency tests use generated OpenAPI as the source of truth. They assert path/method
coverage, representative response documentation for success and failure statuses, and boundary
schemas for public discovery, customer history, business-scoped reservations, and owner-only
membership administration.

Account security hardening tests verify:

- Five failed password sign-in attempts create account-scoped password reset protection.
- Password reset email delivery uses a fake adapter in API integration tests.
- Password sign-in stays blocked until reset succeeds.
- Inactive accounts are denied at request time even with otherwise valid JWTs.
- Inactive businesses or memberships deny protected business actions.
- Public generated documentation and public booking discovery remain reachable.

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

- A separate timeslot service runtime is deferred until a later explicit runtime split and
  outbox/message-broker design.
- Token revocation for redesigned account JWTs is deferred.

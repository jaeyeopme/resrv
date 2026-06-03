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
| Platform exchange architecture tests | `platform-exchange/src/test` | Pure Java exchange boundary and event-package guard |

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
- Platform exchange APIs do not depend on Spring, Jakarta, Hibernate, platform implementation
  packages, or exchange event packages.
- Direct database access primitives stay inside outbound adapter packages in production code.
- Request-handling Web Adapter classes do not depend on endpoint-level Swagger/OpenAPI
  annotations; those annotations live on same-package `*ApiDocs` interfaces implemented by the
  adapters.

Reservation list/search tests verify business membership authorization, business-local date
windows, optional resource/customer/state filters, and deterministic start-time ordering.

Customer reservation history tests verify self-scoped list/detail APIs, owner-only account
filtering, inactive business/resource summary rendering, bounded page/size validation, stable
descending ordering, derived state and `upcoming=true` filtering before pagination, and identical
public `404` responses for missing and not-owned detail, confirm, release, and customer-cancel
lookups.

Business resource lifecycle tests verify full replacement semantics for booking settings, resource
details, booking overrides, weekly schedules, and date overrides. They also cover resource ID-only
identity, duplicate resource names, rejection of obsolete slug/handle request fields, explicit
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
no public resource slug/handle exposure, and business-slug-scoped authenticated hold creation.

Timeslot reservation traffic tests verify generated-slot non-overlap, DST/midnight timezone
boundaries, stale or policy-drifted slot rejection before persistence, advisory-lock ordering before
blocker checks, active blocker overlap semantics, expired hold rows remaining stored while
non-blocking, blocked hold `409 Conflict`, expired-hold confirmation `409 Conflict`, malformed hold
payload `400`, IDOR-safe customer reservation not-found responses, business-access `403`, and
same-slot/same-reservation contention with exactly one successful transition across repeated
attempts.

Platform runtime packaging tests verify that the canonical platform runtime serves booking settings,
public booking discovery, and ticketing API groups; applies platform, timeslot, and ticketing
schemas; rejects inactive accounts for protected booking actions; preserves non-enumerating
wrong-business public slot lookup responses; exposes platform, booking, and ticketing endpoint
groups from generated OpenAPI; excludes unsupported capability groups; verifies public/private
schema boundaries; and checks that human docs do not duplicate a hand-written endpoint catalog.

Operational readiness tests verify public liveness/readiness probes, database-backed readiness,
Flyway migration history visibility for platform, timeslot, and ticketing migrations, generated
OpenAPI reachability for smoke checks, and documentation drift around unsupported standalone
services.

API contract consistency tests use generated OpenAPI as the source of truth. They assert path/method
coverage, representative response documentation for success and failure statuses, and boundary
schemas for public discovery, customer history, business-scoped reservations, and owner-only
membership administration. When endpoint documentation changes, tests should verify generated
summaries or response descriptions rather than inspecting controller annotations directly.

Ticketing API tests run through the platform runtime because ticketing has no standalone backend
runtime. Focused ticketing verification is:

```bash
./gradlew :platform:test --tests '*Ticketing*'
```

Those tests cover generated OpenAPI for purchase confirmation, customer ticket history, and business
ticket activity; unavailable selected seats as `409 Conflict`; idempotency problem reasons
`invalid_retry` and `expired_key`; and non-enumerating not-found responses for missing versus
unauthorized business activity probes. Run `./gradlew :ticketing:test` only when ticketing
application, domain, or persistence behavior changes.

Ticket purchase idempotency uses a 24-hour replay window. Expired idempotency records remain retained
until 30 days after replay expiry and may be cleaned later, but cleanup is not required for purchase
correctness.

## Traffic-Sensitive Feature Review

Use the architecture traffic-pattern guidance before implementing a new
high-contention flow. Review language must preserve each bounded context's own
terms: reservation work can talk about generated slots and active blockers,
while ticketing work can talk about selected-seat ownership and purchase
idempotency.

Review questions:

- Can capacity be overbooked, oversold, or partially claimed under concurrent attempts?
- Does the flow protect generated availability and active blockers, selected ownership, or another
  explicit invariant?
- What repeat-request behavior is expected: replay, invalid retry, expired retry, or new attempt?
- Does expiry release correctness immediately, retain rejection behavior, or only permit cleanup?
- Which lifecycle states are terminal, reversible, expired, or conflict-producing?
- Which public responses must remain stable for losing contention or unauthorized probes?
- Does the feature need a fresh spec or ADR before adding queue, waitlist, payment, notification,
  external calendar, token-revocation, or runtime-split behavior?

Future public behavior changes for traffic-sensitive flows must be visible through the accepted
public contract and covered by end-to-end verification appropriate to the change. API behavior
changes must update generated OpenAPI coverage and API integration tests. Documentation-only pattern
updates should state why runtime tests were not run.

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
- Token revocation for account-scoped JWTs is deferred.

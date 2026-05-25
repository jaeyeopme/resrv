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

## Architecture Rules

ArchUnit verifies:

- Platform domain does not depend on application, adapters, or API runtime.
- Platform domain has no Spring, Jakarta, or Hibernate dependencies.
- Platform application does not depend on adapters or API runtime.
- Timeslot domain does not depend on application, adapters, or API runtime.
- Timeslot domain has no Spring, Jakarta, or Hibernate dependencies.
- Timeslot application does not depend on adapters or API runtime.
- Timeslot does not depend on platform domain, adapters, API runtime, repositories, entities, or
  persistence schema.
- Only the timeslot outbound platform adapter may depend on explicit platform application
  lookup/access contracts.
- Direct database access primitives stay inside outbound adapter packages in production code.

Reservation list/search tests verify business membership authorization, business-local date
windows, optional resource/customer/state filters, and deterministic start-time ordering.

## Testcontainers

Integration and persistence tests use PostgreSQL through Testcontainers. Test properties configure:

```text
spring.datasource.url=jdbc:tc:postgresql:16:///resrv
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver
```

JWT tests use a fixed local test secret and test issuer/audience values.

## Known Gaps

- `timeslot` local boot packaging is pending.
- Login rate limiting and failed-login lockout are deferred.
- Token revocation for redesigned account JWTs is deferred.

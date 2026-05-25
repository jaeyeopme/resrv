# resrv

`resrv` is a Java 25 + Spring Boot 4 backend for a multi-tenant B2B reservation
platform. The redesign branch models platform accounts, businesses, memberships, timeslot booking
settings, resources, schedules, virtual slots, and reservation lifecycle transitions.

The core correctness question is:

> Who reserved which resource, for which business, and for which time range, without leaking
> business data, trusting client-supplied authorization, or allowing active overbooking?

## Current Redesign State

Current modules:

```text
shared-kernel
platform
timeslot
```

- `platform` is the runnable platform API assembly.
- `timeslot` contains the booking API assembly, but its `bootJar` and `bootRun` tasks are currently
  disabled pending final packaging.
- `docs/adr/0001-bounded-context-module-baseline.md` records the superseded 11-module baseline.
- `docs/adr/0017-collapse-to-bounded-context-modules.md` records the active three-module decision.

## Read Order

| Document | Purpose |
|---|---|
| [PRD](docs/prd.md) | Product scope, users, requirements, non-goals |
| [ADR](docs/adr/) | Durable architecture decisions |
| [TRD](docs/trd.md) | Technical design, runtime structure, data model, constraints |
| [Architecture](docs/architecture.md) | Stable high-level system and dependency boundaries |
| [Security](docs/security.md) | JWT, membership authorization, public endpoints, deferred hardening |
| [Testing](docs/testing.md) | Test strategy, quality gates, coverage thresholds |
| [Operations](docs/operations.md) | Local run, environment, database, troubleshooting |
| [Glossary](docs/glossary.md) | Canonical domain terms |

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA |
| API docs | Springdoc OpenAPI, Swagger UI |
| Security | JWT HS256, Argon2 password hashing |
| Build | Gradle 9 |
| Quality | Spotless, Checkstyle, OpenRewrite, JaCoCo, ArchUnit |
| Tests | JUnit 5, Spring Boot tests, Testcontainers |

## Build And Verify

Docker must be running for Testcontainers-backed tests.

```bash
npm ci
npm run hooks:install
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

`./gradlew check` runs compilation, tests, Checkstyle, ArchUnit tests, JaCoCo report generation, and
coverage verification.

## Run Platform API Locally

`platform` can be run with Spring Boot Docker Compose support. It discovers the root
`compose.yml` when started from the repository root.

```bash
RESRV_JWT_SECRET_KEY=01234567890123456789012345678901 \
RESRV_JWT_ISSUER=resrv-dev \
RESRV_JWT_AUDIENCE=resrv-api \
RESRV_JWT_EXPIRATION=3600 \
./gradlew :platform:bootRun
```

Then open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

Generated OpenAPI is the API contract surface. Do not maintain a separate hand-written endpoint
catalog.

Timeslot local runtime packaging remains intentionally disabled as recorded in
[ADR-0014](docs/adr/0014-timeslot-booking-api-boundary.md).

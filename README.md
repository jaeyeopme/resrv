# resrv

[![CI](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml)

`resrv` is a Java 25 + Spring Boot 4 backend for a multi-tenant B2B reservation
platform.

It models platform accounts, businesses, memberships, booking settings,
resources, schedules, virtual slots, and reservation lifecycle transitions. The
main correctness goal is to answer:

> Who reserved which resource, for which business, and for which time range,
> without leaking business data, trusting client-supplied authorization, or
> allowing active overbooking?

## Current State

`v0.1.0-review-baseline` marks a review-ready bounded-context module baseline.
It is not production-complete: payments, staff invitation delivery and acceptance
UI, password reset UI, combined timeslot runtime packaging, deployment
infrastructure, and notification workflows are intentionally out of scope. Core
owner-managed staff membership administration is implemented in the platform API.

The active redesign uses four Gradle modules:

| Module | Role |
|---|---|
| `shared-kernel` | Shared IDs and time primitives |
| `platform-exchange` | Pure Java platform-owned exchange APIs for cross-context lookup/check decisions |
| `platform` | Account, login, business, membership, runnable API |
| `timeslot` | Booking settings, resources, schedules, slots, reservations |

`platform` is runnable today. `timeslot` depends on `platform-exchange` rather
than the platform implementation module, but `timeslot` `bootJar` and `bootRun`
remain disabled until runtime packaging is finalized.

## Project Entry Points

| Document | Purpose |
|---|---|
| [README](README.md) | Repository entry point, current structure, key commands |
| [PRD](docs/prd.md) | Product requirements and open product questions |
| [TRD](docs/trd.md) | Technical requirements and design |
| [Architecture](docs/architecture.md) | Current architecture summary |
| [ADR index](docs/adr/README.md) | Architecture decision record index |
| [AGENTS](AGENTS.md) | Agent working rules, guardrails, build commands |

Supporting docs:

- [Security](docs/security.md)
- [Testing](docs/testing.md)
- [Operations](docs/operations.md)
- [Glossary](docs/glossary.md)

Baseline references and implemented feature artifacts are captured as Spec Kit
specs under `specs/`.

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

Timeslot local runtime packaging remains intentionally disabled. The current
compile-time exchange boundary is recorded in
[ADR-0020](docs/adr/0020-platform-exchange-boundary.md); a real runtime split
needs a later outbox/message-broker design.

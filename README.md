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
UI, password reset UI, production deployment infrastructure, and notification
workflows are intentionally out of scope. Core owner-managed staff membership
administration is implemented in the platform API.

The active redesign uses four Gradle modules:

| Module | Role |
|---|---|
| `shared-kernel` | Shared IDs and time primitives |
| `platform-exchange` | Pure Java platform-owned exchange APIs for cross-context lookup/check decisions |
| `platform` | Account, login, business, membership, canonical runnable API |
| `timeslot` | Booking settings, resources, schedules, slots, reservations contributed to the platform runtime |

`platform` is the canonical backend runtime and serves both platform and booking
API groups. `timeslot` depends on `platform-exchange` rather than platform
implementation packages, and its `bootJar` and `bootRun` tasks remain disabled
so it does not become a second supported backend runtime accidentally.

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
- [Spec Kit usage](docs/spec-kit.md)

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

`platform` is the supported local backend runtime. It can be run with Spring Boot
Docker Compose support and discovers the root `compose.yml` when started from the
repository root.

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
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

Generated OpenAPI from the platform runtime is the API contract surface. Keep human docs at the
API-group and policy level; do not maintain a separate hand-written endpoint catalog.

For production-like runs, use `SPRING_PROFILES_ACTIVE=prod` with explicit datasource, JWT, and
password reset settings. See [Operations](docs/operations.md) for the full configuration and smoke
check path.

Build the executable runtime package:

```bash
./gradlew :platform:bootJar
```

Build the local container image with Jib:

```bash
./gradlew :platform:jibDockerBuild
```

The local image name is `resrv-platform-api:latest`.

Timeslot standalone runtime packaging remains intentionally disabled. The
current runtime decision is recorded in
[ADR-0022](docs/adr/0022-platform-runtime-packaging.md); a real runtime split
needs a later outbox/message-broker design.

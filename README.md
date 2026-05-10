# resrv — Multi-tenant Reservation API

[![CI](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml)

`resrv` is a multi-tenant B2B reservation API built around practical backend delivery: product requirements translated into API contracts, tenant isolation, JWT security, reservation lifecycle rules, PostgreSQL concurrency guarantees, and Testcontainers-backed verification.

This is intentionally more than CRUD. The project proves that a backend can answer a real operational question consistently: **who reserved which resource, for which tenant, and for which time range — without leaking tenant data or allowing active overbooking.**

## Why this is more than basic CRUD

| Signal | Evidence |
|---|---|
| Product-to-backend delivery | Tenant onboarding, admin/customer auth, resources, availability, slot search, holds, confirmation, cancellation, and admin audit are implemented end to end. |
| Non-CRUD domain complexity | Availability exceptions, tenant timezones, hold TTL, reservation state transitions, and no-overbooking constraints are part of the working flow. |
| Security and tenancy boundaries | JWT `tenantId` drives authenticated tenant scope; URL tenant slugs are resolved server-side for public login/signup flows. |
| Data correctness | PostgreSQL `EXCLUDE USING gist` constraint blocks overlapping active reservations even under concurrent requests. |
| Verification discipline | Gradle `check` runs tests, Checkstyle, ArchUnit, JaCoCo, and Testcontainers-backed integration scenarios in CI. |

## How to inspect it

| If you have... | Start here | What you should see |
|---|---|---|
| 30 seconds | This README + [`docs/status.md`](docs/status.md) | Implemented scope, CI status, and why the project goes beyond basic CRUD. |
| 3 minutes | [`docs/case-study.md`](docs/case-study.md) | Problem → design choices → implementation evidence → verification, kept short. |
| Architecture review time | [`docs/architecture.md`](docs/architecture.md), [`docs/api.md`](docs/api.md), [`docs/decisions.md`](docs/decisions.md) | Hexagonal boundaries, tenancy/security model, endpoint surface, and durable decisions. |
| Local API review | `./gradlew :bootstrap:bootRun` then `/swagger-ui.html` | Public OpenAPI/Swagger surface with mutating `Try it out` disabled by default. |

## What is implemented now

| Area | Implemented capability |
|---|---|
| Tenant onboarding | Tenant creation with the first `OWNER` administrator |
| Auth | Administrator login, customer login, JWT issuance, logout JTI blacklist, `/me` |
| Resource management | Tenant-scoped resource create/read/update/deactivate |
| Availability | Weekly recurring hours and date-specific closures/special hours |
| Slot search | Available-slot calculation using tenant timezone and slot duration |
| Reservation lifecycle | Customer-authenticated hold → confirm → customer cancel |
| Admin audit | Administrator view of reservations per resource |
| No overbooking | PostgreSQL `EXCLUDE USING gist` constraint blocks active reservation overlaps |
| API docs | Public Springdoc OpenAPI JSON/YAML and Swagger UI |

## Evidence map

| Question | Evidence |
|---|---|
| What product problem does it model? | [`docs/product.md`](docs/product.md) |
| How do I inspect the API? | [`docs/api.md`](docs/api.md), `/swagger-ui.html`, `/v3/api-docs`, `/v3/api-docs.yaml` |
| How is the system structured? | [`docs/architecture.md`](docs/architecture.md) |
| What tradeoffs were intentional? | [`docs/decisions.md`](docs/decisions.md) |
| What is complete vs deferred? | [`docs/status.md`](docs/status.md) |
| What is the short case-study narrative? | [`docs/case-study.md`](docs/case-study.md) |

## Architecture at a glance

```text
adapter-web         adapter-persistence
     \                  /
      \                /
        application
            |
          domain

bootstrap: Spring Boot assembly, Security/JWT/OpenAPI, integration tests
```

- `domain`: framework-free domain model and invariants
- `application`: use cases, port interfaces, and transaction boundaries
- `adapter-web`: REST controllers, DTOs, validation, and authenticated-principal mapping
- `adapter-persistence`: JPA, Flyway migrations, and PostgreSQL constraints
- `bootstrap`: runtime assembly, Security/JWT/OpenAPI configuration, and integration tests

The dependency direction is `adapter-* -> application -> domain`. See [`docs/architecture.md`](docs/architecture.md) for details.

## Tech stack

| Category | Technology |
|---|---|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| API docs | Springdoc OpenAPI, Swagger UI |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA |
| Security | JWT HS256, Argon2id password hashing, PostgreSQL-backed JTI revocation blacklist with scheduled cleanup |
| Build / quality | Gradle 9, Spotless, Checkstyle, JaCoCo, ArchUnit, commitlint, Lefthook |
| Tests | JUnit 5, Testcontainers |

## Run locally

### Prerequisites

- JDK 25+
- Node.js 24+ for repository tooling hooks
- Docker running for PostgreSQL/Testcontainers

### Install local Git hooks

```bash
npm ci
npm run hooks:install
```

Commit subjects are validated with commitlint through Lefthook and in CI. Use Conventional Commit subjects such as `fix(auth): persist logout revocation`.

### Start the API

```bash
./gradlew :bootstrap:bootRun
```

`bootRun` starts PostgreSQL through the root `compose.yml` and uses a built-in development JWT secret so the API can be reviewed without extra setup. Set `JWT_SECRET_KEY` to a 32+ byte value for any shared, staged, or production-like environment.

Keep the terminal open while reviewing the API. Startup is complete when the log prints `Started ResrvApplication`; because `bootRun` is a long-running server task, it does not return `BUILD SUCCESSFUL` until the process exits. If you stop it with `Ctrl-C` or a forced kill, Gradle may report a non-zero exit such as `130` or `143` even though startup already succeeded.

Then open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

Swagger UI is public for review, but mutating `Try it out` is disabled by default in `application.yml`.

## Main API flow

1. `POST /api/tenants` — create a tenant and first `OWNER` admin.
2. `POST /public/{tenantSlug}/auth/login` — log in as an admin and receive a Bearer token.
3. `POST /api/resources` — create a reservable resource.
4. `PUT /api/resources/{resourceId}/weekly-availability/{dayOfWeek}` — set weekly hours.
5. `POST /public/{tenantSlug}/customers` — register a customer.
6. `POST /public/{tenantSlug}/customers/login` — log in as a customer.
7. `GET /api/resources/{resourceId}/slots?date=YYYY-MM-DD` — list available slots.
8. `POST /api/reservation-holds` — hold a slot as the logged-in customer.
9. `POST /api/reservation-holds/{reservationId}/confirm` — confirm the hold.
10. `POST /api/me/reservations/{reservationId}/cancel` — cancel a customer-owned reservation.

Detailed endpoint and payload notes are in [`docs/api.md`](docs/api.md).

## Verification

```bash
npm run commitlint
./gradlew spotlessApply
./gradlew check
```

`npm run commitlint` validates the latest commit message. `check` runs compilation, unit/slice/integration tests, Checkstyle, ArchUnit, JaCoCo coverage verification/report generation, and Testcontainers-backed checks. Docker must be running for the Testcontainers portion.

## Inspection checklist

- Start from Swagger UI and verify that the API surface is discoverable.
- Read [`docs/case-study.md`](docs/case-study.md) for the short problem/design/evidence flow.
- Read [`docs/product.md`](docs/product.md) to understand why customer login is mandatory for reservations.
- Inspect [`docs/architecture.md`](docs/architecture.md) for hexagonal boundaries and ArchUnit enforcement.
- Inspect `adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql` for DB-level no-overbooking.
- Inspect `adapter-persistence/src/main/resources/db/migration/V8__create_revoked_token.sql` for scale-out JWT revocation.
- Inspect `bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java` for the end-to-end reservation scenario.

## Explicitly deferred

The following hardening items are intentionally deferred and should not be treated as accidental gaps: login rate limiting, failed-login lockout, and tenant/admin active-state validation filter. See [`docs/status.md`](docs/status.md).

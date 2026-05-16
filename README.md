# resrv — Multi-tenant Reservation API

[![CI](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml)

`resrv` is a multi-tenant B2B reservation API built to show end-to-end backend product delivery: requirements translated into API contracts, tenant isolation, JWT security, reservation lifecycle rules, PostgreSQL consistency guarantees, and Testcontainers-backed verification.

The core operational question is: **who reserved which resource, for which tenant, and for which time range — without leaking tenant data, allowing unauthorized actors, or permitting active overbooking?**

## What reviewers can verify

| Review angle | What to inspect | Evidence |
|---|---|---|
| Product problem | The project models a real reservation workflow with tenants, operators, customers, resources, availability, and reservation states. | [`docs/product.md`](docs/product.md), [`docs/case-study.md`](docs/case-study.md) |
| Design choices | Tenant scope, admin/customer roles, JWT boundaries, reservation states, and database constraints are explicit decisions. | [`docs/architecture.md`](docs/architecture.md), [`docs/decisions.md`](docs/decisions.md) |
| Implementation | Public onboarding, admin operations, customer reservations, persistence, and runtime assembly are wired end to end. | [`docs/api.md`](docs/api.md), [`docs/status.md`](docs/status.md) |
| Verification | Unit, slice, integration, architecture, coverage, and CI checks back the main claims. | [`bootstrap/src/test/java/io/resrv/bootstrap`](bootstrap/src/test/java/io/resrv/bootstrap), [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

## How to inspect it

| If you have... | Start here | What you should see |
|---|---|---|
| 30 seconds | This README + [`docs/status.md`](docs/status.md) | Implemented scope, current evidence, and intentionally deferred hardening. |
| 3 minutes | [`docs/case-study.md`](docs/case-study.md) | Problem → design choices → implementation evidence → verification, kept short. |
| Architecture review time | [`docs/architecture.md`](docs/architecture.md), [`docs/api.md`](docs/api.md), [`docs/decisions.md`](docs/decisions.md) | Module boundaries, tenancy/security model, endpoint surface, and durable decisions. |
| Local API review | `./gradlew :bootstrap:bootRun` then `/swagger-ui.html` | Public OpenAPI/Swagger surface with mutating `Try it out` disabled by default. |

## Implemented scope

| Area | Implemented capability |
|---|---|
| Tenant onboarding | Tenant creation with the first `OWNER` administrator |
| Auth | Administrator login, customer login, JWT issuance, logout JTI blacklist, `/me` |
| Resource management | Tenant-scoped resource create/read/update/deactivate |
| Availability | Weekly recurring hours and date-specific closures/special hours |
| Slot search | Available-slot calculation using tenant timezone and slot duration |
| Reservation lifecycle | Customer-authenticated hold → confirm → list → customer cancel |
| Admin operations | Resource reservation audit, tenant-wide reservation search, admin cancel, check-in, and no-show |
| No overbooking | PostgreSQL `EXCLUDE USING gist` constraint blocks active reservation overlaps |
| API docs | Public Springdoc OpenAPI JSON/YAML and Swagger UI |

## Tech stack

| Category | Technology |
|---|---|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| API docs | Springdoc OpenAPI, Swagger UI |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA |
| Security | JWT HS256, Argon2id password hashing, PostgreSQL-backed JTI revocation blacklist with scheduled cleanup |
| Build / quality | Gradle 9, OpenRewrite, Spotless, Checkstyle, JaCoCo, ArchUnit, commitlint, Lefthook |
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

## Representative API flow

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
11. `GET /api/reservations?date=YYYY-MM-DD&resourceId=&customerId=&status=` — search tenant reservations as an operator.
12. `POST /api/reservations/{reservationId}/admin-cancel`, `/check-in`, or `/no-show` — apply bounded operator lifecycle transitions when the reservation state and time window allow it.

Detailed endpoint and payload notes are in [`docs/api.md`](docs/api.md).

## Verification

```bash
npm run commitlint
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

`npm run commitlint` validates the latest commit message. `rewriteDryRun` previews the active OpenRewrite cleanup recipe without changing files. `check` runs compilation, unit/slice/integration tests, Checkstyle, ArchUnit, JaCoCo coverage verification/report generation, and Testcontainers-backed checks. Docker must be running for the Testcontainers portion.

## Reviewer checklist

- Read [`docs/case-study.md`](docs/case-study.md) for the short problem → design → implementation → verification narrative.
- Inspect [`docs/api.md`](docs/api.md) or Swagger UI to see the public, admin, and customer workflow surfaces.
- Inspect [`docs/architecture.md`](docs/architecture.md) for module boundaries, tenant/auth model, data model, and ArchUnit enforcement.
- Inspect [`adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql`](adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql) for DB-level no-overbooking.
- Inspect [`application/src/main/java/io/resrv/application/reservation`](application/src/main/java/io/resrv/application/reservation) and [`adapter-web/src/main/java/io/resrv/adapter/in/web/reservation`](adapter-web/src/main/java/io/resrv/adapter/in/web/reservation) for reservation use cases and REST adapters.
- Inspect [`bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java`](bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java), [`application/src/test/java/io/resrv/application/reservation`](application/src/test/java/io/resrv/application/reservation), and [`adapter-web/src/test/java/io/resrv/adapter/in/web/reservation`](adapter-web/src/test/java/io/resrv/adapter/in/web/reservation) for workflow and boundary verification.

## Explicitly deferred

The following hardening items are intentionally deferred and should not be treated as accidental gaps: login rate limiting, failed-login lockout, and tenant/admin active-state validation filter. See [`docs/status.md`](docs/status.md).

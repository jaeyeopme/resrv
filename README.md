# resrv — Multi-tenant B2B Reservation API

`resrv` is a multi-tenant B2B reservation API for businesses that manage reservable resources and for customers who sign in, find available slots, and hold/confirm/cancel reservations.

The review value is not simple CRUD. The project connects tenant isolation, role-based JWT authentication, availability calculation, hold-based reservation lifecycle, PostgreSQL concurrency constraints, OpenAPI documentation, and Testcontainers-backed integration tests into one backend system.

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

## Quick links

| Need | Location |
|---|---|
| API surface / Swagger | [`docs/api.md`](docs/api.md), `/swagger-ui.html`, `/v3/api-docs`, `/v3/api-docs.yaml` |
| Product intent and MVP boundary | [`docs/product.md`](docs/product.md) |
| Architecture and module boundaries | [`docs/architecture.md`](docs/architecture.md) |
| Current status and next steps | [`docs/roadmap.md`](docs/roadmap.md) |
| Decision records | [`docs/decisions.md`](docs/decisions.md) |
| Internal execution wiki | [`omx_wiki/`](omx_wiki/README.md) |

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
| Security | JWT HS256, Argon2id password hashing, Caffeine JTI blacklist |
| Build / quality | Gradle 9, Spotless, Checkstyle, JaCoCo, ArchUnit |
| Tests | JUnit 5, Testcontainers |

## Run locally

### Prerequisites

- JDK 25+
- Docker running for PostgreSQL/Testcontainers

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
./gradlew spotlessApply
./gradlew check
```

`check` runs compilation, unit/slice/integration tests, Checkstyle, ArchUnit, JaCoCo coverage verification/report generation, and Testcontainers-backed checks. Docker must be running for the Testcontainers portion.

## Inspection checklist

- Start from Swagger UI and verify that the API surface is discoverable.
- Read [`docs/product.md`](docs/product.md) to understand why customer login is mandatory for reservations.
- Inspect [`docs/architecture.md`](docs/architecture.md) for hexagonal boundaries and ArchUnit enforcement.
- Inspect `adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql` for DB-level no-overbooking.
- Inspect `bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java` for the end-to-end reservation scenario.

## Explicitly deferred

The following hardening items are intentionally deferred and should not be treated as accidental gaps: login rate limiting, failed-login lockout, tenant/admin active-state validation filter, and persistent DB/Redis JTI blacklist. See [`docs/roadmap.md`](docs/roadmap.md).

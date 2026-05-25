# Architecture

## Overview

`resrv` is a Spring Boot 4 application organized as five Gradle subprojects around Hexagonal Architecture (Ports & Adapters).

```text
adapter-web         adapter-persistence
     \                  /
      \                /
        application
            |
          domain

bootstrap: runtime assembly, Security/JWT/Password/OpenAPI adapters, integration tests
```

## Subproject responsibilities

| Subproject | Responsibility | Guardrail |
|---|---|---|
| `domain` | Domain model, value objects, invariants | No Spring/JPA dependency |
| `application` | Use cases, input/output ports, transaction boundaries | Do not leak web/JPA DTOs inward |
| `adapter-web` | REST requests/responses, validation, authenticated-principal mapping | Do not implement business rules directly |
| `adapter-persistence` | JPA entities, repositories, mappers, Flyway migrations | No dependency on the web layer |
| `bootstrap` | Application assembly, Security/JWT/Password implementation, OpenAPI configuration, integration tests | Do not hide domain rules in configuration code |

## Dependency direction

- `domain` is a pure Java domain model.
- `application` uses `domain` and defines ports.
- `adapter-web` and `adapter-persistence` attach to application ports.
- `bootstrap` owns Spring Boot runtime assembly and integration tests.
- ArchUnit continuously verifies the intended dependency direction.

## Multi-tenancy

- The database uses a shared-database model.
- Tenant-scoped tables are isolated by `tenant_id`.
- Authenticated APIs use the JWT `tenantId` claim as the server-side tenant boundary.
- Public login/signup APIs resolve the URL `tenantSlug` to a server-side `tenantId`.
- Tenant IDs sent by clients in request bodies are not trusted.
- Tenant-scoped unique constraints must include `tenant_id`.

## Authentication and authorization boundary

| API group | Tenant source | Auth | Role boundary |
|---|---|---|---|
| `POST /api/tenants` | Newly created | Public | None |
| `/public/{tenantSlug}/auth/login` | URL slug | Public credential | Tenant admin login |
| `/public/{tenantSlug}/customers` | URL slug | Public credential | Customer registration/login |
| `/api/auth/**` | JWT `tenantId` | Bearer JWT | Authenticated user |
| Resource CRUD | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |
| Availability write | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |
| Slot search | JWT `tenantId` | Bearer JWT | Admin or customer |
| Customer reservation | JWT `tenantId` | Bearer JWT | `CUSTOMER` |
| Admin reservation operations | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |

JWTs are self-issued HS256 tokens. Access tokens live for 30 minutes, and refresh tokens are outside the MVP scope. Logout writes the token `jti` to a PostgreSQL-backed `revoked_token` table so revocation works across application instances. Expired revocation rows are ignored during authentication and removed by a scheduled cleanup using `resrv.auth.revocation-cleanup-interval`.

## Data model

The current Flyway schema includes:

| Table | Key fields / constraints |
|---|---|
| `tenant` | slug, timezone, slot duration, hold TTL, cancellation window, status |
| `admin` | tenant_id, email, hashed_password, role, active |
| `resource` | tenant_id, slug, name, description, status, created_at, updated_at |
| `customer` | tenant_id, email, name, hashed_password, active, created_at |
| `resource_weekly_availability` | tenant_id, resource_id, day_of_week, start_time, end_time |
| `resource_availability_exception` | tenant_id, resource_id, date, closed, optional start/end time |
| `reservation` | tenant_id, resource_id, customer_id, start_at, end_at, status, hold/confirm/cancel timestamps |
| `revoked_token` | jti, expires_at, revoked_at |

Important constraints:

- Tenant slug is globally unique.
- Admin email, customer email, and resource slug are unique within a tenant.
- Resource status is `ACTIVE` or `INACTIVE`.
- Reservation status is `HELD`, `CONFIRMED`, `CUSTOMER_CANCELLED`, `ADMIN_CANCELLED`, `CHECKED_IN`, `NO_SHOW`, or `EXPIRED`.
- `reservation` uses PostgreSQL `btree_gist` and `tstzrange(start_at, end_at, '[)')` exclusion constraints to prevent overlap between active reservations.

## Reservation correctness

Reservation correctness has three layers:

1. **Availability calculation**: Date exceptions take precedence; otherwise weekly rules are used. Tenant local date/time is calculated in the tenant timezone and converted to UTC `Instant` slots.
2. **Application guard**: Before creating a hold, the service checks active overlaps and confirms the requested time is an available slot. Expired holds are marked `EXPIRED` when reservation workflows run.
3. **Database guard**: Even if concurrent requests pass the application guard, the PostgreSQL exclusion constraint prevents overlapping `HELD`, `CONFIRMED`, and `CHECKED_IN` reservations.

Reservation time ranges are interpreted as half-open intervals: `[start, end)`.

## OpenAPI and public review surface

- Springdoc exposes `/v3/api-docs` and `/v3/api-docs.yaml`.
- Swagger UI is available at `/swagger-ui.html`.
- OpenAPI documentation endpoints allow public access.
- Swagger UI mutating `Try it out` is disabled by default.

## Test strategy

| Level | Location | Purpose |
|---|---|---|
| Domain tests | `domain/src/test` | Validate value objects and domain invariants |
| Application tests | `application/src/test` | Validate use cases and port interactions |
| Web adapter tests | `adapter-web/src/test` | Validate REST contracts, validation, and security boundaries |
| Persistence tests | `adapter-persistence/src/test` | Validate JPA/Flyway/PostgreSQL integration |
| Integration tests | `bootstrap/src/test` | Validate key flows after real application assembly |
| Architecture tests | `bootstrap/src/test/.../architecture` | Validate module dependency direction and layering rules |

Representative integration tests:

- `AuthIntegrationTest`: login/logout/me behavior and revoked-token rejection
- `ResourceManagementIntegrationTest`: admin login and Resource CRUD flow
- `ReservationMvpIntegrationTest`: customer registration/login, availability, slots, hold/confirm/cancel, admin reservation search/lifecycle operations, and no-overbooking flow
- `OpenApiIntegrationTest`: OpenAPI/Swagger public access and current API path exposure

## Explicitly deferred hardening

The following security/operations hardening items are deferred to Phase 2 and should not be implemented without an explicit request.

| ID | Item |
|---|---|
| T100 | Login rate limiting |
| T101 | Failed-login counter and lockout |
| T102 | `UserStateValidationFilter` enforcing active tenant/admin state |
| T103 | Active hold quota per customer |
| T104 | Reservation hold/status model cleanup |
| T105 | JPA enum mapping cleanup with `@Enumerated(EnumType.STRING)` |

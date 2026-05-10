# Case Study — From reservation rules to verified backend behavior

This is a short technical narrative. It is not meant to replace the API or architecture docs; it explains why the implemented pieces matter together.

## 1. Problem

A reservation backend must coordinate multiple concerns at once:

- isolate each tenant's data and operators;
- let administrators configure resources and bookable hours;
- let customers search, hold, confirm, list, and cancel reservations;
- prevent overlapping active reservations even when requests race;
- expose an API surface that readers and integrators can inspect.

The hard part is not creating rows. The hard part is keeping identity, time, tenant scope, reservation state, and data constraints consistent across the whole flow.

## 2. Design choices

| Concern | Choice | Why it matters |
|---|---|---|
| Tenant boundary | Shared database with tenant-scoped records and JWT `tenantId` as the authenticated boundary | Client-supplied tenant ids are not trusted for authenticated APIs. |
| API shape | Public tenant slug for signup/login; JWT for authenticated business APIs | Public flows remain tenant-addressable while protected flows use server-issued claims. |
| Domain flow | Customer account required for reservation hold/confirm/cancel | Reservation ownership, cancellation, and audit history need identity. |
| Architecture | Hexagonal modules: `domain`, `application`, `adapter-web`, `adapter-persistence`, `bootstrap` | Business rules stay away from web/JPA details and module direction is testable. |
| Overbooking guard | Application overlap check plus PostgreSQL exclusion constraint | The database remains the final correctness boundary under concurrent requests. |

See [`decisions.md`](decisions.md) for the durable decision summary.

## 3. Implementation evidence

| Behavior | Where to inspect |
|---|---|
| Tenant onboarding and admin login | `RegisterTenantWebAdapter`, `LoginWebAdapter`, `LoginService` |
| Customer registration/login | `CustomerWebAdapter`, `CustomerService` |
| Resource and availability management | `ResourceWebAdapter`, `AvailabilityWebAdapter` |
| Slot search and reservation lifecycle | `ReservationWebAdapter`, `ReservationService` |
| No-overbooking constraint | `adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql` |
| Public OpenAPI surface | [`api.md`](api.md), `/swagger-ui.html`, `/v3/api-docs` |

## 4. Verification evidence

| Verification | Evidence |
|---|---|
| End-to-end reservation flow | `bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java` |
| Resource management flow | `bootstrap/src/test/java/io/resrv/bootstrap/ResourceManagementIntegrationTest.java` |
| OpenAPI exposure | `bootstrap/src/test/java/io/resrv/bootstrap/OpenApiIntegrationTest.java` |
| Module boundaries | ArchUnit tests under `bootstrap/src/test/java/io/resrv/bootstrap/architecture` |
| CI quality gate | GitHub Actions runs `./gradlew check --no-daemon` |

## 5. What this demonstrates

`resrv` demonstrates the ability to carry a backend feature from product rules through API design, security boundaries, persistence constraints, and automated verification. The value is not the number of endpoints, but the way the endpoints, tenant model, reservation rules, and tests reinforce each other.

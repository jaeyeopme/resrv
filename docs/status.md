# Current Status and Future Work

This page is a public status summary, not a planning log. It foregrounds implemented evidence first, then records polish opportunities and intentionally deferred hardening.

## Current milestone

The Phase 1 reservation MVP is implemented enough for external readers to judge core backend quality from API behavior, architecture boundaries, tests, and database constraints.

## Implemented foundation

- 5-subproject hexagonal architecture
- PostgreSQL/Flyway schema migrations
- Spring Security JWT resource server
- PostgreSQL-backed JWT revocation blacklist
- Tenant onboarding
- Tenant admin login/logout/me
- Customer registration/login
- Resource management
- Availability rules and exceptions
- Reservation hold/confirm/list/cancel flow
- Admin reservation audit and bounded operator reservation search
- Springdoc OpenAPI/Swagger UI
- ProblemDetail error responses
- Testcontainers-backed integration-test structure

## Implemented reservation evidence

| Capability | Evidence |
|---|---|
| Customer registration/login | `CustomerWebAdapter`, `CustomerService`, `customer` table |
| Admin-only Resource management | `ResourceWebAdapter` role guard |
| Admin-only Availability write | `AvailabilityWebAdapter` role guard |
| Authenticated slot search | `GET /api/resources/{resourceId}/slots` |
| Customer reservation hold | `POST /api/reservation-holds` |
| Customer reservation confirm | `POST /api/reservation-holds/{reservationId}/confirm` |
| Customer reservation list/cancel | `/api/me/reservations/**` |
| Admin reservation audit | `GET /api/resources/{resourceId}/reservations` |
| Admin reservation operator search | `GET /api/reservations?date=YYYY-MM-DD&...` |
| Admin reservation lifecycle controls | `admin-cancel`, `check-in`, and `no-show` reservation transitions |
| DB-level no-overbooking | `V7__create_reservation.sql` exclusion constraint |
| Scale-out token revocation | `V8__create_revoked_token.sql`, `TokenRevocationPersistenceAdapter`, `RevokedTokenCleanup` |
| End-to-end flow test | `ReservationMvpIntegrationTest` |
| OpenAPI surface test | `OpenApiIntegrationTest` |
| CI quality gate | `.github/workflows/ci.yml` runs commitlint and `./gradlew check --no-daemon` |
| OpenAPI reviewer metadata | Controller annotations for tags, summaries, auth requirements, status codes, and schema fields |

## Product decisions already settled

- Only logged-in customers can create reservations.
- Guest reservation tokens are not part of the MVP.
- Resource capacity is 1 in the MVP.
- Overbooking prevention uses both application checks and a PostgreSQL exclusion constraint.
- Logout revocation is persisted in PostgreSQL instead of process-local memory.
- Swagger/OpenAPI remains the public review surface.

## High-signal polish opportunities

These items would improve review ergonomics, but they are not required to understand the current backend evidence.

| Priority | Item | Reason |
|---|---|---|
| P1 | Add operation-level OpenAPI request/response examples | Reviewers can reproduce the demo flow from Swagger alone |
| P1 | Add a compact curl walkthrough | Reviewers can quickly follow the local success path without bloating the root README |
| P1 | Add more reservation domain/application unit tests | State-transition rules become fast to verify without Testcontainers |
| P2 | Add seed/demo profile | Reduces demo setup time |
| P2 | Publish read-only hosted demo policy | Provides a safer external review path than a mutable demo environment |

## Explicitly deferred hardening

The following authentication/operations items are intentionally outside the current milestone. They are recorded so reviewers can distinguish deliberate scope control from accidental omissions.

| ID | Item | Reason |
|---|---|---|
| T100 | Login rate limiting | Requires operations policy and storage/infrastructure choice |
| T101 | Failed-login lockout | Requires user-state model and unlock policy |
| T102 | Tenant/admin active-state validation filter | Requires state-transition policy and API error contract |

## Longer-term realism expansions

- Payment or deposit integration port
- Email/SMS notification outbox
- More granular staff permissions
- Customer profile update/password reset
- Recurring/package reservations
- Observability: structured logging, metrics, trace id
- Read-only hosted demo and public Swagger operating policy

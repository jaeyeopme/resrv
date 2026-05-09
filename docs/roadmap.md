# Roadmap

## Phase 1 — Review Reservation MVP

Phase 1 aims to let external reviewers judge the core reservation-backend quality from the API, tests, and implementation evidence.

### Completed foundation

- 5-subproject hexagonal architecture
- PostgreSQL/Flyway schema migrations
- Spring Security JWT resource server
- Tenant onboarding
- Tenant admin login/logout/me
- Resource management
- Springdoc OpenAPI/Swagger UI
- ProblemDetail error responses
- Testcontainers-backed integration-test structure

### Completed reservation MVP

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
| DB-level no-overbooking | `V7__create_reservation.sql` exclusion constraint |
| End-to-end flow test | `ReservationMvpIntegrationTest` |
| OpenAPI surface test | `OpenApiIntegrationTest` |

## Product decisions already settled

- Only logged-in customers can create reservations.
- Guest reservation tokens are not part of the MVP.
- Resource capacity is 1 in the MVP.
- Overbooking prevention uses both application checks and a PostgreSQL exclusion constraint.
- Swagger/OpenAPI remains the public review surface.

## Phase 1 polish backlog

These items would further improve review quality.

| Priority | Item | Reason |
|---|---|---|
| P1 | Add OpenAPI example payloads | Reviewers can reproduce the demo flow from Swagger alone |
| P1 | Add a README curl walkthrough | Reviewers can quickly follow the local success path |
| P1 | Add more reservation domain/application unit tests | State-transition rules become fast to verify without Testcontainers |
| P2 | Add admin reservation status transition API | Operators can manage check-in/no-show/admin-cancel states |
| P2 | Add seed/demo profile | Reduces demo setup time |
| P2 | Document CI badge and GitHub Actions | Provides public trust signal |

## Phase 2 — Deferred security and operations hardening

The following items are authentication/operations hardening work, separate from the current feature phase. Do not implement them without an explicit request.

| ID | Item | Reason |
|---|---|---|
| T100 | Login rate limiting | Requires operations policy and storage/infrastructure choice |
| T101 | Failed-login lockout | Requires user-state model and unlock policy |
| T102 | Tenant/admin active-state validation filter | Requires state-transition policy and API error contract |
| T103 | Persistent JTI blacklist | Requires DB/Redis choice and operations cost decision |

## Phase 3 — Service realism expansion

- Payment or deposit integration port
- Email/SMS notification outbox
- More granular staff permissions
- Customer profile update/password reset
- Recurring/package reservations
- Observability: structured logging, metrics, trace id
- Read-only hosted demo and public Swagger operating policy

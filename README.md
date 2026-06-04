# resrv

[![CI](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jaeyeopme/resrv/actions/workflows/ci.yml)

`resrv` is a Spring Boot and PostgreSQL backend for scarce-capacity reservation
and ticketing workflows.

It focuses on the backend problems that make these domains hard to implement
well: preventing duplicate reservation holds, preventing selected-seat oversell,
making repeated purchase confirmations safe, and resolving business access on
the server instead of trusting client-provided tenant or role data.

The project demonstrates a modular-monolith API with bounded-context modules,
generated OpenAPI, PostgreSQL persistence, account-scoped security,
transactional contention handling, and automated quality gates.

## At A Glance

| Area | Current state |
|---|---|
| Purpose | Scarce-capacity API for reservation holds and selected-seat ticket purchases |
| Runtime | One supported Spring Boot API from the `platform` module |
| Language | Java 25 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA |
| API contract | Generated OpenAPI from the running platform API |
| Modules | `platform`, `timeslot`, `ticketing`, `platform-exchange`, `shared-kernel` |
| Verification | Gradle `check`, Testcontainers, ArchUnit, JaCoCo, Checkstyle, Spotless, OpenRewrite |

## What This Shows

The repository is structured to make core backend design choices easy to inspect:

- **Bounded contexts**: platform identity, booking, ticketing, exchange APIs,
  and shared primitives are separated by Gradle modules and package rules.
- **Authorization model**: JWTs identify only the account. Business access,
  reservation ownership, and ticket activity access are resolved server-side.
- **Contention correctness**: reservation holds use PostgreSQL advisory
  locks and active blocker checks; ticket purchase confirmation uses
  all-or-nothing selected-seat ownership and idempotency replay.
- **API contract discipline**: generated OpenAPI is the endpoint and schema
  contract; hand-written endpoint catalogs are avoided.
- **Quality gates**: tests cover domain behavior, application use cases,
  persistence, API flows, runtime wiring, architecture rules, and coverage
  thresholds.

## Quick Start

Prerequisites:

- JDK 25.
- Docker, because integration tests use Testcontainers.
- Node 24 if you want commitlint and Lefthook installed locally.

Install repository tooling:

```bash
npm ci
npm run hooks:install
```

Run the main verification path:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

Run the API locally:

```bash
./gradlew :platform:bootRun
```

When no active Spring profile is set, `bootRun` uses the local profile. Start it
from the repository root so Spring Boot Docker Compose support can discover
`compose.yml`.

Open the generated API and health surfaces:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

## Implemented Scope

| Area | Implemented behavior |
|---|---|
| Identity | Account registration, login, account-scoped JWTs, inactive-account rejection |
| Account recovery | Repeated failed password sign-in protection and password reset challenge completion |
| Business access | Business creation, owner membership, staff grant/list/audit/update/disable |
| Booking setup | Booking settings, resources, weekly schedules, date override schedules |
| Public booking | Business discovery, active resource discovery, generated slots, authenticated hold creation |
| Reservations | Hold, confirm, release, customer cancel, business cancel, check-in, no-show, customer history, business search |
| Ticketing | Ticket events, sale windows, tiered inventory, selected seats, purchase confirmation, customer history, business activity |
| Runtime | One platform API serving platform, booking, and ticketing API groups |

Terminology:

- **Booking** means the broader scheduling workflow: settings, resources,
  schedules, generated slots, discovery, and hold creation.
- **Reservation** means the persisted time-range record and lifecycle facts:
  hold, confirm, release, cancel, check-in, and no-show.

## Architecture

The app runs as one Spring Boot process from the `platform` module. `timeslot`
and `ticketing` contribute booking and ticketing behavior to that process.
`platform-exchange` is a plain Java module for cross-context lookup and access
decisions. It is not HTTP, messaging, or an outbox layer.

The runtime path is intentionally simple:

1. API clients call the `platform` Spring Boot app.
2. The platform runtime assembles platform, booking, and ticketing API groups.
3. `timeslot` and `ticketing` ask platform-owned lookup and access questions
   through `platform-exchange`.
4. Each bounded context owns its PostgreSQL schema and migrations.

Module roles:

| Module | Role |
|---|---|
| `shared-kernel` | Shared identity and timezone primitives |
| `platform-exchange` | Pure Java APIs used by other modules to ask platform for business and access decisions |
| `platform` | Accounts, login, businesses, memberships, security, migrations, and the runnable API |
| `timeslot` | Booking settings, resources, schedules, generated slots, and reservations |
| `ticketing` | Ticket events, inventory, selected seats, purchases, history, and business activity |

`timeslot` and `ticketing` do not depend on platform implementation packages or
platform persistence tables. They use `platform-exchange` for platform-owned
facts and access decisions. Their local `bootRun` and `bootJar` tasks remain
disabled, so there is one supported backend runtime.

More detail: [docs/architecture.md](docs/architecture.md) and
[docs/trd.md](docs/trd.md).

## Correctness Examples

### Reservation Holds

Hold creation starts from a public business slug. The platform context resolves
the active business, then timeslot validates the resource, slot identity, and
capacity before saving a hold. The hold path revalidates generated slot identity
against current settings and schedule data, then protects the resource/time
range with a PostgreSQL advisory lock before checking active blockers.

The write path is:

1. Resolve the active business from the public slug.
2. Decode and revalidate the opaque `slotId`.
3. Lock the resource and slot start in PostgreSQL.
4. Check active blockers.
5. Save the hold only when capacity is still available.

Reservation state is derived from timestamp facts on the reservation row. Held,
confirmed, and checked-in reservations block capacity. Expired, released,
cancelled, and no-show reservations do not.

### Ticket Purchases

Ticketing models selected-seat purchases as the first durable lifecycle action:

- A successful confirmation creates one ticket purchase and marks every selected
  seat as purchased.
- Multi-seat confirmation is all-or-nothing.
- Contending customers cannot oversell a selected seat.
- Customer-scoped idempotency keys replay the original public outcome for 24
  hours.
- Changed same-key retries and retained expired keys return stable public
  problem reasons.

## API Contract

The platform runtime generates the API contract:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`

The repository does not maintain a hand-written `docs/api.md` endpoint catalog
or a committed OpenAPI snapshot. Narrative docs describe product scope,
architecture, security boundaries, and testing strategy. Exact paths, methods,
schemas, and response documentation come from generated OpenAPI.

## Build Evidence

Evidence is produced by local build and runtime commands instead of committed as
standalone snapshots:

| Evidence | How to generate | Where to inspect |
|---|---|---|
| OpenAPI contract | `./gradlew :platform:bootRun` | `/v3/api-docs`, `/v3/api-docs.yaml`, Swagger UI |
| Test reports | `./gradlew check` | `*/build/reports/tests/test/index.html` |
| Coverage reports | `./gradlew check` | `*/build/reports/jacoco/test/html/index.html` |
| Checkstyle reports | `./gradlew check` | `*/build/reports/checkstyle/*.html` |
| High-contention API behavior | `./gradlew :platform:test --tests '*Concurrency*' --tests '*HighContention*'` | Platform test report |
| Executable API jar | `./gradlew :platform:bootJar` | `platform/build/libs/resrv-platform-api-0.0.1-SNAPSHOT.jar` |
| Local container image | `./gradlew :platform:jibDockerBuild` | `resrv-platform-api:latest` |

No extra standalone artifact file is required now. Static OpenAPI snapshots,
Postman collections, exported ERD images, and operations guides would duplicate
generated OpenAPI, Flyway migrations, runtime health probes, or the existing
design documents. Presentation and sales assets belong outside this repository.

## Architecture References

Architecture detail lives in the durable docs and ADRs:

| Reference | Location |
|---|---|
| Runtime and bounded-context map | [docs/architecture.md](docs/architecture.md#bounded-contexts) |
| Module dependency map | [docs/architecture.md](docs/architecture.md#current-module-state) |
| Persistence ownership map | [docs/architecture.md](docs/architecture.md#persistence-access-policy) |
| Contention correctness catalog | [docs/architecture.md](docs/architecture.md#high-contention-correctness-guidance) |

## Quality Gates

The primary check sequence is:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

`check` covers compilation, tests, Checkstyle, ArchUnit tests, JaCoCo report
generation, and coverage verification. CI also runs commitlint and CodeQL.

Focused checks:

```bash
./gradlew :platform:test --tests '*Ticketing*'
./gradlew :platform:test --tests '*Concurrency*' --tests '*HighContention*'
./gradlew :platform:test --tests io.resrv.platform.api.PlatformRuntimePackagingIntegrationTest
./gradlew :platform:test --tests io.resrv.platform.api.PlatformOperationalReadinessIntegrationTest
```

## Documentation Map

| Document | Purpose |
|---|---|
| [docs/prd.md](docs/prd.md) | Product scope, concepts, flows, acceptance criteria, and open product questions |
| [docs/trd.md](docs/trd.md) | Current technical design, runtime, persistence, security, and configuration reference |
| [docs/architecture.md](docs/architecture.md) | Bounded contexts, module boundaries, and contention correctness patterns |
| [docs/security.md](docs/security.md) | Authentication, authorization, public exposure, data boundaries, and deferred hardening |
| [docs/testing.md](docs/testing.md) | Test strategy, quality gates, coverage thresholds, and focused verification commands |
| [docs/adr/README.md](docs/adr/README.md) | Architecture decision record index |

## Project Boundaries

Current non-goals:

- Load benchmarking, traffic simulation, and production capacity planning.
- Payments, deposits, invoices, and refunds.
- Staff invitation delivery and acceptance UI.
- Password reset UI. Backend challenge completion exists.
- Notifications and reminders, except SMTP-compatible password reset delivery.
- External calendar sync.
- Separate `timeslot` or `ticketing` runtimes.
- Message broker, outbox, event projections, and production runtime splitting.
- Ticketing checkout attempts, failed-attempt persistence, holds, cancellations,
  expiration records, waitlists, resale, public marketing discovery, and seating
  map editing.

# Implementation Plan: Account Security Hardening

**Branch**: `007-account-security-hardening` | **Date**: 2026-05-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/007-account-security-hardening/spec.md`

## Summary

Add production-grade account sign-in protection while preserving the account-scoped token model.
Platform owns failed password tracking, email-triggered password reset, reset completion, and
request-time active account checks. Timeslot continues to resolve business and membership state
server-side through explicit platform contracts and keeps public discovery reachable while excluding
inactive businesses/resources from bookable results.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA, Flyway, Springdoc OpenAPI, Spring Mail, PostgreSQL driver
**Storage**: PostgreSQL 16 via Flyway-managed `platform` and `timeslot` schemas
**Testing**: JUnit 5, Spring Boot tests, Testcontainers PostgreSQL, Spring Security test, ArchUnit, JaCoCo
**Target Platform**: Server-side JVM API
**Project Type**: Multi-module web service API
**Performance Goals**: Sign-in protection checks complete within the normal sign-in request path; public booking discovery must not add account-security lookups for anonymous callers
**Constraints**: Preserve account-scoped tokens with no business role claims; keep timeslot from reading platform tables directly; do not add a handwritten endpoint catalog
**Scale/Scope**: One production-readiness feature spanning platform auth/security and timeslot public discovery behavior

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Boundary-first design**: PASS. Account security state, password reset, email delivery, and
  active account checks stay in `platform`; booking discovery changes stay in `timeslot`.
  Timeslot uses explicit `platform.contract` types for platform state.
- **Server-side authorization**: PASS. Tokens remain account-scoped. Business access remains
  resolved through active membership or reservation ownership.
- **Generated API contract**: PASS. Contract changes are documented as OpenAPI deltas for
  generated docs and integration tests. No handwritten endpoint catalog is introduced.
- **Reservation correctness**: PASS. The feature does not change slot ID binding, hold blockers,
  reservation lifecycle, or booking policy math.
- **Quality gates**: PASS. Implementation must finish with `./gradlew spotlessApply`,
  `./gradlew rewriteDryRun`, and `./gradlew check`.

## Project Structure

### Documentation (this feature)

```text
specs/007-account-security-hardening/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── email-delivery.md
│   └── openapi-delta.md
└── tasks.md              # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
platform/
├── src/main/java/io/resrv/platform/adapter/in/web/auth/
├── src/main/java/io/resrv/platform/adapter/in/web/error/
├── src/main/java/io/resrv/platform/adapter/out/persistence/account/
├── src/main/java/io/resrv/platform/adapter/out/email/
├── src/main/java/io/resrv/platform/api/security/
├── src/main/java/io/resrv/platform/application/auth/
├── src/main/java/io/resrv/platform/application/account/
├── src/main/java/io/resrv/platform/application/security/
├── src/main/java/io/resrv/platform/contract/account/
├── src/main/resources/db/migration/
└── src/test/java/io/resrv/platform/

timeslot/
├── src/main/java/io/resrv/timeslot/adapter/in/web/resource/
├── src/main/java/io/resrv/timeslot/adapter/in/web/slot/
├── src/main/java/io/resrv/timeslot/adapter/out/platform/
├── src/main/java/io/resrv/timeslot/application/resource/
├── src/main/java/io/resrv/timeslot/application/slot/
└── src/test/java/io/resrv/timeslot/

docs/
└── adr/
```

**Structure Decision**: Keep all new security state and reset behavior inside `platform`.
Add only contract-facing lookup types needed by `timeslot`, following the existing
`platform.contract` pattern. Add an outbound email adapter under `platform` so SMTP delivery is
replaceable without domain/application dependency leaks; tests use a fake adapter.

## Phase 0: Research

See [research.md](research.md).

## Phase 1: Design & Contracts

See [data-model.md](data-model.md), [contracts/openapi-delta.md](contracts/openapi-delta.md),
[contracts/email-delivery.md](contracts/email-delivery.md), and [quickstart.md](quickstart.md).

## Post-Design Constitution Check

- **Boundary-first design**: PASS. Planned persistence adapters and email adapters are outbound
  platform adapters. Timeslot only depends on explicit platform contract types.
- **Server-side authorization**: PASS. Active account/business/member checks happen server-side;
  token claims remain limited to account identity.
- **Generated API contract**: PASS. API behavior changes are tied to generated OpenAPI and
  integration tests; contracts here describe deltas, not a maintained endpoint catalog.
- **Reservation correctness**: PASS. Public discovery filtering uses existing active
  business/resource semantics and does not alter reservation state transitions.
- **Quality gates**: PASS. Plan defines focused tests and repository-wide verification.

## Complexity Tracking

No constitution violations.

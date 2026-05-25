<!--
Sync Impact Report
Version change: 1.0.0 -> 1.0.1
Modified principles:
- III. Generated API Contract: clarified documentation focus with normative language
Added sections:
- Sync Impact Report
Removed sections: None
Templates requiring updates:
- ✅ .specify/templates/plan-template.md
- ✅ .specify/templates/spec-template.md
- ✅ .specify/templates/tasks-template.md
- ✅ .specify/templates/checklist-template.md reviewed; no constitution-specific update required
- ✅ .specify/extensions/git/commands/*.md reviewed; no outdated agent-specific references found
- ✅ .specify/templates/commands/*.md not present
- ✅ README.md reviewed; Spec Kit usage intentionally omitted
- ✅ AGENTS.md
Follow-up TODOs: None
-->

# resrv Constitution

## Core Principles

### I. Boundary-First Design

`resrv` is organized by bounded context. Platform owns accounts, businesses,
memberships, authentication, and membership checks. Timeslot owns booking
settings, resources, schedules, virtual slots, and reservations. Shared-kernel
must stay small and limited to stable primitives.

Hexagonal dependency direction is required:

```text
api/runtime -> adapters -> application -> domain
```

Domain code must not depend on Spring, JPA, adapters, application services, or
runtime assembly. Timeslot must not read platform persistence directly; it uses
explicit `platform.contract` types through its outbound platform adapter.

### II. Server-Side Authorization

JWTs identify platform accounts only. They must not carry business role,
tenant-local role, or business authorization claims.

Business authorization is resolved server-side through active
`BusinessMembership`. Customer reservation transitions require reservation
ownership. Business reservation transitions require active owner or staff
membership.

### III. Generated API Contract

Generated OpenAPI and Swagger are the API contract surface. Do not maintain a
hand-written endpoint catalog.

When API behavior changes, update controllers, DTOs, validation, Springdoc
annotations, and API integration tests together. Human documentation MUST focus
on product, architecture, security, testing, and operations context rather than
duplicating endpoint tables.

### IV. Reservation Correctness

Slots are virtual and generated from schedules, business timezone, and effective
booking policy. Slot IDs must bind to business, resource, start time, and end
time.

Hold creation must reject stale, malformed, wrong-business, wrong-resource, or
unavailable slots. Active blockers are unexpired holds, confirmed reservations,
and checked-in reservations. Expired holds stop blocking without requiring a
cleanup mutation.

### V. Quality Gates Are Part Of The Design

All non-trivial changes must preserve formatting, architecture rules, tests, and
coverage gates.

Expected verification:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

Docker must be running for Testcontainers-backed tests.

## Technology Constraints

- Java 25
- Spring Boot 4
- Spring MVC and Spring Security
- PostgreSQL 16
- Flyway
- Spring Data JPA for owned persistence
- PostgreSQL advisory locks only from outbound persistence adapters when needed
- Gradle 9 multi-module build
- JUnit 5, Spring Boot tests, Testcontainers, ArchUnit, JaCoCo

## Source Of Truth

| Surface | Role |
|---|---|
| `docs/prd.md` | Product scope and acceptance criteria |
| `docs/trd.md` | Current technical design |
| `docs/architecture.md` | Stable architecture overview |
| `docs/security.md` | Authentication and authorization rules |
| `docs/testing.md` | Quality gates and test strategy |
| `docs/operations.md` | Local run and operational notes |
| `docs/adr/` | Durable decision records |
| `specs/` | Spec Kit feature specs created for new work |

ADRs supersede planning artifacts for durable technical decisions. Spec Kit
plans and tasks must be reconciled with ADRs before implementation.

## Governance

This constitution guides Spec Kit work in this repository. Amend it when the
project's operating rules change, and keep `AGENTS.md` and README navigation in
sync.

Breaking architecture changes require an ADR. API behavior changes require API
integration tests and generated OpenAPI coverage. Deferred Phase 2 work must not
be implemented unless explicitly requested.

**Version**: 1.0.1 | **Ratified**: 2026-05-26 | **Last Amended**: 2026-05-26

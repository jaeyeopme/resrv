# Spec Kit Usage

Spec Kit is used in `resrv` to make feature work explicit before implementation.
It does not replace PRD/TRD documents, ADRs, generated OpenAPI, or architecture
tests.

## Source Of Truth

| Surface | Role |
|---|---|
| `docs/prd.md` | Product scope, users, requirements, and non-goals |
| `docs/trd.md` | Technical design and current architecture constraints |
| `docs/adr/` | Durable technical decisions |
| Generated OpenAPI | API contract |
| `.specify/memory/constitution.md` | Spec Kit governance gates |
| `specs/` | Feature-specific planning artifacts |

ADRs are authoritative for durable architecture decisions. If a Spec Kit output
conflicts with an accepted ADR, revise the spec/plan or update the ADR before
implementation.

## Normal Feature Flow

Use this flow for new features, significant behavior changes, and planned
refactors:

```text
$speckit-specify
$speckit-clarify   # optional when the spec has material ambiguity
$speckit-plan
$speckit-tasks
$speckit-analyze
$speckit-implement
```

`$speckit-analyze` only runs after a feature has `spec.md`, `plan.md`, and
`tasks.md`. It is read-only and reports consistency issues across those files.

## Existing Behavior Baselines

Because `resrv` already has substantial implementation and ADR coverage,
baseline specs are allowed when they improve clarity for existing behavior.

Baseline specs must:

- Stay scoped to one bounded context or capability.
- Describe current behavior from PRD/TRD/ADRs/code.
- Avoid replacing ADRs or generated OpenAPI.
- Avoid creating implementation tasks unless a real change is required.
- Split discovered gaps into separate feature specs before implementation.

Do not create a single broad retrospective spec for the entire system. Prefer
small baseline specs that can become stable references for later changes.

Recommended baseline order:

| Baseline | Main Inputs |
|---|---|
| Platform account and authentication | ADR-0003, ADR-0005, ADR-0006, `docs/security.md` |
| Business and membership authorization | ADR-0004, ADR-0013, `docs/security.md` |
| Booking settings and policy resolution | ADR-0007, ADR-0008, `docs/trd.md` |
| Resource schedules and virtual slots | ADR-0009, ADR-0010, `docs/architecture.md` |
| Reservation lifecycle and locking | ADR-0011, ADR-0012, ADR-0013, `docs/testing.md` |
| Timeslot booking API boundary | ADR-0014, ADR-0015, ADR-0016 |

## Baseline Spec Shape

Baseline specs should still use normal Spec Kit sections, but their scope is
documentation and validation of current behavior.

Use these conventions:

- Feature name suffix: `baseline` where helpful, for example
  `platform-auth-baseline`.
- User stories describe the actors who rely on the current behavior.
- Functional requirements describe observed or ADR-mandated behavior.
- Success criteria focus on clarity and verifiability, not delivery metrics.
- Assumptions identify behavior inferred from code rather than explicit docs.
- `plan.md` may be light if no implementation change is planned.
- `tasks.md` may contain documentation/test-gap tasks only when gaps are found.

If a baseline uncovers unclear or incorrect behavior, create a new focused
feature spec for the change rather than mixing behavior change into the
baseline.

## Spec Status

Use `Baseline` for existing-behavior reference specs that intentionally have no
implementation tasks. Use `Pending` for candidate feature specs that have no
`plan.md` or `tasks.md` yet. Use `Implemented` only after a task-backed feature
has all tasks completed and the implementation has been merged.

## Implemented Review Priorities

The review-cleanup queue below has already been implemented. Treat it as a
historical checkpoint list, not as the next work queue.

| Spec | Focus | Completion Bar |
|---|---|---|
| `013-runtime-packaging` | Runtime packaging | Implemented platform and timeslot APIs run from an intentional, documented deployment shape. |
| `014-api-contract-consistency` | API contract consistency | Generated OpenAPI, API tests, error responses, and public/private boundaries match implemented behavior. |
| `015-minimal-operations` | Minimal operations | Production profile, migration execution, health checks, and packaging instructions are enough to run the backend predictably. |
| `016-data-authorization-consistency` | Data and authorization consistency | Membership, business, resource, and reservation state changes preserve access rules and IDOR-sensitive response policy. |
| `017-web-adapter-docs` | Web adapter documentation separation | Request-handling Web Adapters stay focused on HTTP mapping while generated OpenAPI metadata lives on matching `*ApiDocs` interfaces. |
| `018-timeslot-traffic-hardening` | Timeslot reservation traffic hardening | Hold creation, lifecycle transitions, expiration behavior, and conflict responses are concurrency-safe and covered by API tests. |

Current `Pending` feature specs:

| Spec | Focus | Next Step |
|---|---|---|
| `019-ticket-event-inventory-model` | Ticket event and inventory baseline | Implement open tasks before ticket hold/claim traffic work starts. |

## Future Feature Roadmap

Use this roadmap to preserve intended direction without creating stale
`spec.md`/`plan.md`/`tasks.md` artifacts too early. Create full Spec Kit
artifacts only when the feature becomes the next implementation target.

| Candidate | Phase | Focus | Start After | Notes |
|---|---|---|---|---|
| `020-ticket-purchase-hold-lifecycle` | Phase 2 | Ticket hold, purchase/confirm, release, expiration, customer ownership, and business access. | `019-ticket-event-inventory-model` implemented | Proves ticket traffic has an actual mutable lifecycle, not only static event inventory. |
| `021-ticket-concurrency-hardening` | Phase 2 | Oversell prevention, high-contention hold/purchase behavior, idempotency key, retry-safe commands, and concurrency tests. | Ticket lifecycle endpoints and persistence exist | This is the main evidence that the backend handles a second traffic-sensitive flow. |
| `022-ticket-api-contract-and-operations` | Phase 2 | Ticket API contract, OpenAPI coverage, integration tests, operational notes, and failure response consistency. | Ticket lifecycle and concurrency behavior stabilized | Keep generated OpenAPI authoritative; avoid hand-written endpoint catalogs. |
| `023-traffic-pattern-extraction` | Phase 3 | Cross-flow backend patterns for capacity allocation, hold expiration, retry safety, lock/claim strategy, queue/backpressure, and lifecycle/state machine design. | Timeslot and ticket traffic both implemented and tested | Extract patterns from working flows; do not introduce speculative shared abstractions before this point. |

Future runtime-split, outbox/message-broker, payments, notifications, external
calendar sync, UI, or token-revocation work needs a fresh feature spec before
planning.

## Extension Hooks

Local `.specify/extensions.yml`, `.specify/extensions/`, and
`.specify/extension-catalogs.yml` state are ignored by Git. Disabling the analyze
hook suppresses `/speckit.analyze` commit prompts only; other Spec Kit steps can
still show their own enabled hook prompts.

## Implementation Rules

- Read the active feature's `plan.md` before implementation.
- Keep generated specs aligned with PRD/TRD and ADRs.
- For API behavior changes, update Web Adapters, DTOs, generated OpenAPI/API integration tests,
  and the matching same-package `*ApiDocs` interface for endpoint-level Springdoc metadata.
- Keep HTTP mapping and request binding on Web Adapters. Keep payload field schema metadata on
  payload types. Mirror method-validation annotations on `*ApiDocs` only when Bean Validation
  requires interface and implementation method declarations to match.
- Preserve bounded-context and hexagonal package boundaries.
- Run project verification after implementation:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

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

## Extension Hooks

Local `.specify/extensions.yml` hook settings are ignored by Git. Disabling the
analyze hook suppresses `/speckit.analyze` commit prompts only; other Spec Kit
steps can still show their own enabled hook prompts.

## Implementation Rules

- Read the active feature's `plan.md` before implementation.
- Keep generated specs aligned with PRD/TRD and ADRs.
- Update API tests and Springdoc annotations for API behavior changes.
- Preserve bounded-context and hexagonal package boundaries.
- Run project verification after implementation:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

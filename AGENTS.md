# AGENTS.md

## Project Snapshot

`resrv` is a multi-tenant B2B reservation API built with Java 25, Spring Boot 4,
PostgreSQL, and Gradle. The codebase uses bounded-context modules with hexagonal
package boundaries.

Current Gradle modules:

```text
shared-kernel
platform
timeslot
```

`platform` is the runnable Spring Boot API. `timeslot` contains booking API code,
but its `bootJar` and `bootRun` tasks are intentionally disabled until runtime
packaging is decided.

## Required Commands

Run formatting before full verification:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

Run the platform API locally:

```bash
RESRV_JWT_SECRET_KEY=01234567890123456789012345678901 \
RESRV_JWT_ISSUER=resrv-dev \
RESRV_JWT_AUDIENCE=resrv-api \
RESRV_JWT_EXPIRATION=3600 \
./gradlew :platform:bootRun
```

Tests use Testcontainers. Docker must be running.

## Project Entry Points

| File | Purpose |
|---|---|
| `README.md` | Repository entry point, current structure, key commands |
| `docs/prd.md` | Product requirements and open product questions |
| `docs/trd.md` | Technical requirements and design |
| `docs/architecture.md` | Current architecture summary |
| `docs/adr/README.md` | Architecture decision record index |
| `AGENTS.md` | Repository automation rules, guardrails, build commands |

Supporting docs:

- `docs/security.md`
- `docs/testing.md`
- `docs/operations.md`
- `docs/glossary.md`
- `docs/spec-kit.md`
- `.specify/memory/constitution.md`
- `specs/` when Spec Kit feature specs exist

ADRs are the decision record. Generated OpenAPI is the endpoint contract.
Spec Kit specs may drive new work, but they do not replace ADRs or generated API
contracts.

## Spec Kit

Read `docs/spec-kit.md` before starting or reviewing Spec Kit work.

Use this workflow for spec-driven work:

- `$speckit-constitution`
- `$speckit-specify`
- `$speckit-clarify`
- `$speckit-plan`
- `$speckit-tasks`
- `$speckit-analyze`
- `$speckit-implement`

Keep generated specs aligned with `docs/prd.md`, `docs/trd.md`, and ADRs. Spec
Kit outputs may guide implementation, but accepted ADRs and generated OpenAPI
remain authoritative for durable decisions and API contracts.

## API Contract

Generated OpenAPI/Swagger is the API contract surface. Do not create or maintain a hand-written
`docs/api.md` endpoint catalog.

When changing API behavior:

- Update controllers, DTOs, validation, and Springdoc annotations where needed.
- Update or add API integration tests.
- Let `/v3/api-docs`, `/v3/api-docs.yaml`, and `/swagger-ui.html` expose the contract.
- Keep human docs focused on product, architecture, security, operations, and testing context.
- Mention only high-level API groups in narrative docs unless a concrete endpoint example is needed
  to explain a decision.

## Authorization Responses

- IDOR-sensitive object lookups must not reveal whether a probed object id exists when the caller
  lacks authority to access it.
- For those lookups, missing objects and existing objects outside caller authority must return the
  same not-found style public response; keep any cause distinction internal and non-sensitive.

## Persistence Access

- Production code defaults to Spring Data JPA for owned persistence.
- Direct database access primitives (`EntityManager`, `DataSource`, native SQL) belong only in
  outbound adapter packages.
- Test `JdbcTemplate` use is allowed for fixtures and database assertions.
- If production code needs native SQL/JDBC, document why JPA is not the right fit.
- Timeslot must not read platform tables directly. Use explicit `platform-exchange` APIs from the
  timeslot outbound platform adapter.

## Commit Messages

Match the repository's existing Conventional Commit subject style before committing.

Subject format:

```text
<type>(optional-scope): <intent summary>
```

Rules:

- Inspect recent `git log --oneline` before committing; do not invent a new subject style.
- Do not use Title Case imperative subjects such as `Document ...` or `Make ...`.
- Preferred types: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `chore`, `ci`, `perf`, `style`, `revert`.
- Prefer narrow scopes when obvious: `auth`, `api`, `docs`, `build`, `nullness`, etc.
- Commit subjects are enforced with commitlint + Lefthook; run `npm ci && npm run hooks:install` after cloning.
- Keep commit bodies short and why-oriented. Skip the body when the subject is self-explanatory.
- Add a body only for non-obvious rationale, breaking changes, data migrations, security fixes, reversions, migration notes, or linked issues.
- Prefer one concise context/rationale paragraph plus `Tested:` when useful.
- Use `Constraint:`, `Rejected:`, `Directive:`, and `Not-tested:` only when they prevent future confusion.
- Do not use `Confidence:` or `Scope-risk:` in normal commit messages.
- Never include file lists, AI attribution, or work-log narration in commit messages.

## Pull Request Descriptions

Use `.github/pull_request_template.md` for PR bodies.

PR descriptions should explain reviewer-relevant context:

- `Summary`: what changed at the product or system level.
- `Motivation`: why the change exists, including ADR or Spec Kit references when useful.
- `Changes`: the concrete behavioral, schema, config, or documentation changes.
- `Verification`: commands run and meaningful manual probes.
- `Risk And Follow-Up`: migration risk, compatibility concerns, known gaps, or next branches.

Keep PR bodies detailed enough for client review, but do not turn them into a chronological work log.
Do not include AI attribution, generated-by footers, model names, or AI co-author trailers.

## Durable Learnings

> One-liner rules added after PR corrections. Format: `- <rule>`

- Do not duplicate booking policy fallback logic across services; resolve it through the domain policy type.

<!-- SPECKIT START -->
When working from a Spec Kit plan, read the active plan before implementation.
Active plan: specs/008-staff-membership-management/plan.md
<!-- SPECKIT END -->

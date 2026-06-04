# AGENTS.md

## Project snapshot

`resrv` is a Java 25 + Spring Boot 4 backend for business reservation workflows.
It models accounts, businesses, staff access, booking setup, generated slots,
reservation lifecycle transitions, ticket events, selected seats, and ticket
purchase confirmation. The codebase uses bounded-context modules with hexagonal
package boundaries.

Current Gradle modules:

```text
shared-kernel
platform-exchange
platform
ticketing
timeslot
```

`platform` is the canonical runnable Spring Boot API and serves platform,
booking, and ticketing API groups. `platform-exchange` contains pure Java
platform-owned lookup/check APIs for cross-context consumers. `timeslot`
contains booking API code contributed to the platform runtime. `ticketing`
contains event, inventory, selected-seat, purchase, history, and business
activity behavior contributed to the platform runtime. `timeslot` and
`ticketing` `bootJar` and `bootRun` tasks are intentionally disabled so they do
not become additional supported backend runtimes.

## Required commands

Run formatting before full verification:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

Run the platform API locally:

```bash
./gradlew :platform:bootRun
```

Tests use Testcontainers. Docker must be running.

## Project entry points

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

ADRs are the decision record. Generated OpenAPI is the endpoint contract.

## API contract

Generated OpenAPI/Swagger is the API contract surface. Do not create or maintain a hand-written
`docs/api.md` endpoint catalog.

When changing API behavior:

- Update Web Adapters for HTTP mapping, request binding, validation, and behavior.
- Put endpoint-level Springdoc metadata such as `@Operation` and `@ApiResponse` on same-package
  `*ApiDocs` interfaces implemented by the matching Web Adapter.
- Mirror method-validation annotations on `*ApiDocs` only when Bean Validation inheritance rules
  require the interface declaration to match the implementing Web Adapter method.
- Keep DTO/payload schema metadata on the payload type when it describes fields.
- Update or add API integration tests.
- Let `/v3/api-docs`, `/v3/api-docs.yaml`, and `/swagger-ui.html` expose the contract.
- Keep human docs focused on product, architecture, security, and testing context.
- Mention only high-level API groups in narrative docs unless a concrete endpoint example is needed
  to explain a decision.

## Authorization responses

- IDOR-sensitive object lookups must not reveal whether a probed object id exists when the caller
  lacks authority to access it.
- For those lookups, missing objects and existing objects outside caller authority must return the
  same not-found style public response; keep any cause distinction internal and non-sensitive.

## Persistence access

- Production code defaults to Spring Data JPA for owned persistence.
- Direct database access primitives (`EntityManager`, `DataSource`, native SQL) belong only in
  outbound adapter packages.
- Test `JdbcTemplate` use is allowed for fixtures and database assertions.
- If production code needs native SQL/JDBC, document why JPA is not the right fit.
- Timeslot and ticketing must not read platform tables directly. Use explicit
  `platform-exchange` APIs from their outbound platform adapters.

## Commit messages

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
- Never include file lists, tool attribution, or work-log narration in commit messages.

## Pull request descriptions

Use `.github/pull_request_template.md` for PR bodies.

PR descriptions should explain reviewer-relevant context:

- `Summary`: what changed at the product or system level.
- `Motivation`: why the change exists, including ADR references when useful.
- `Changes`: the concrete behavioral, schema, config, or documentation changes.
- `Verification`: commands run and meaningful manual probes.
- `Risk And Follow-Up`: migration risk, compatibility concerns, known gaps, or next branches.

Keep PR bodies detailed enough for client review, but do not turn them into a chronological work log.
Do not include tool attribution, generated-by footers, model names, or co-author trailers.

## Durable learnings

> One-liner rules added after PR corrections. Format: `- <rule>`

- Do not duplicate booking policy fallback logic across services; resolve it through the domain policy type.

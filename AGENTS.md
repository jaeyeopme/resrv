# AGENTS.md

> Team-owned — commit every discovery. Personal overrides: `AGENTS.local.md` (.gitignored).

## Project

Multi-tenant B2B SaaS reservation API. Hexagonal (Ports & Adapters), 5 Gradle subprojects, Java 25 + Spring Boot 4 + PostgreSQL.

## Build Commands

```bash
./gradlew spotlessApply      # Auto-format (run before check)
./gradlew check              # Full build + tests + Checkstyle + ArchUnit + JaCoCo coverage gate
./gradlew :bootstrap:bootRun # Run locally — uses compose.yml and a dev JWT fallback
```

Tests use Testcontainers — Docker must be running.

## Document Sources of Truth

| Surface | Role |
|---|---|
| `README.md` | Short external entry point and navigation hub |
| `docs/` | Curated product, architecture, roadmap, and decision docs |
| `omx_wiki/` | Internal execution state, current-state notes, drift tracking, next actions |
| `AGENTS.md` | Agent operating rules, build commands, non-goals, and durable project caveats |

Do not reintroduce Spec Kit as a source of truth. Do not recreate `.specify/`, `specs/`, `.github/agents/speckit.*`, `.github/prompts/speckit.*`, or Spec Kit skill/reference copies unless the user explicitly requests a new Spec Kit setup.

## Phase 2 — Do Not Implement

Deferred by design. Do not add code for these unless explicitly asked:

| ID | Feature |
|---|---|
| T100 | Rate limiting on login |
| T101 | Login lock (`failed_attempts`, `locked_until`) |
| T102 | `UserStateValidationFilter` — enforce `TenantStatus.ACTIVE` and admin active |

## Commit Messages

Match the repository's existing Conventional Commit subject style before committing.

Subject format:

```text
<type>(optional-scope): <lowercase intent summary>
```

Rules:

- Inspect recent `git log --oneline` before committing; do not invent a new subject style.
- Do not use Title Case imperative subjects such as `Document ...` or `Make ...`.
- Preferred types: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `chore`.
- Prefer narrow scopes when obvious: `auth`, `api`, `docs`, `build`, `nullness`, etc.
- Keep the body as a concise decision record with useful Lore trailers:
  `Constraint:`, `Rejected:`, `Confidence:`, `Scope-risk:`, `Directive:`, `Tested:`, `Not-tested:`.
- If subject style and Lore guidance appear to conflict, preserve the Conventional Commit subject style and put the decision rationale in the body/trailers.

## Learnings

> One-liner rules added after PR corrections. Format: `- <rule>`

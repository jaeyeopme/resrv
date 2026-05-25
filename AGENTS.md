# AGENTS.md

> Team-owned — commit every discovery. Personal overrides: `AGENTS.local.md` (.gitignored).

## Project

Multi-tenant B2B SaaS reservation API. Hexagonal (Ports & Adapters), 5 Gradle subprojects, Java 25 + Spring Boot 4 + PostgreSQL.

## Build Commands

```bash
./gradlew spotlessApply      # Auto-format (run before check)
./gradlew rewriteDryRun      # Preview active OpenRewrite cleanup recipes
./gradlew check              # Full build + tests + Checkstyle + ArchUnit + JaCoCo coverage gate
./gradlew :bootstrap:bootRun # Run locally — uses compose.yml and a dev JWT fallback
```

Tests use Testcontainers — Docker must be running.

## Document Sources of Truth

| Surface | Role |
|---|---|
| `README.md` | Short external entry point and navigation hub |
| `docs/` | Curated product, architecture, roadmap, and decision docs |
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

Use the user-level `caveman-commit` skill whenever writing or rewriting commit messages.
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

## Learnings

> One-liner rules added after PR corrections. Format: `- <rule>`

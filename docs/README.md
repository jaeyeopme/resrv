# resrv docs

This directory contains the externally shareable product and technical documentation for `resrv`. It is not an archive of raw planning artifacts; it keeps curated material that helps readers understand what is implemented, why the design choices exist, and where to inspect evidence.

## Documentation contract

| Document | Role |
|---|---|
| [`case-study.md`](case-study.md) | Lightweight problem → design choices → implementation evidence → verification narrative |
| [`product.md`](product.md) | Product intent, users, domain terms, MVP boundary, and business rules |
| [`api.md`](api.md) | Swagger/OpenAPI locations, authentication model, implemented API surface, and review flow |
| [`architecture.md`](architecture.md) | Module structure, multi-tenancy, authentication, data/concurrency model, and test strategy |
| [`status.md`](status.md) | Current implementation evidence, polish opportunities, and explicitly deferred hardening |
| [`decisions.md`](decisions.md) | Summary of durable architecture decisions |

## Writing principles

- Keep the root `README.md` focused on fast evaluation and navigation.
- Prefer current implementation evidence over future ambition.
- Keep `docs/` curated and safe to share externally.
- Keep internal execution state, drift notes, and next-action logs out of public documentation.
- Avoid unverifiable marketing language; every strong claim should point to code, tests, migrations, or architecture notes.

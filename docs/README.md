# resrv docs

This directory contains the externally shareable product and technical documentation for `resrv`. It is not an archive of raw planning artifacts; it only keeps curated material that matches the current codebase and roadmap.

## Documentation contract

| Document | Role |
|---|---|
| [`product.md`](product.md) | Product intent, users, domain terms, MVP boundary, and business rules |
| [`api.md`](api.md) | Swagger/OpenAPI locations, authentication model, implemented API surface, and review flow |
| [`architecture.md`](architecture.md) | Module structure, multi-tenancy, authentication, data/concurrency model, and test strategy |
| [`roadmap.md`](roadmap.md) | Current implementation status and next development phases |
| [`decisions.md`](decisions.md) | Summary of durable architecture decisions |

## Writing principles

- Keep the root `README.md` focused on quick understanding and navigation.
- Keep `docs/` curated and safe to share externally.
- Keep internal execution state, drift notes, and next-action logs out of public documentation.
- Prefer implemented code, tests, and Flyway migrations over stale planning notes when describing current behavior.

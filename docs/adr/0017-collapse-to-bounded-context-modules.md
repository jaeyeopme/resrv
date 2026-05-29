# ADR-0017: Collapse To Bounded-Context Gradle Modules

## Status

Accepted.

## Date

2026-05-25

## History

- Supersedes [ADR-0001](0001-bounded-context-module-baseline.md) as the active module decision.
- `dfefabf refactor(build): collapse context modules`
- `fee8676 fix(platform): hide lookup wiring`
- `78cc495 build: restore package coverage gates`

## Context

ADR-0001 recorded the implemented redesign baseline: 11 Gradle modules split by bounded context and
hexagonal layer.

That structure proved the boundaries, but it also created high overhead:

- Many Gradle projects for a still-small product.
- More dependency declarations.
- More coverage thresholds.
- More navigation cost.
- More friction while the model is still changing.

The user expectation for this stage is closer to:

```text
shared-kernel
platform
timeslot
```

## Decision

Collapse layer-level Gradle modules into bounded-context Gradle modules:

```text
shared-kernel
platform
timeslot
```

Keep hexagonal layers as Java package boundaries:

```text
platform.domain
platform.application
platform.adapter.in.web
platform.adapter.out.persistence
platform.api

timeslot.domain
timeslot.application
timeslot.adapter.in.web
timeslot.adapter.out.persistence
timeslot.adapter.out.platform
timeslot.api
```

Use ArchUnit to enforce:

- Domain must not depend on application, adapters, API runtime, Spring, JPA, or Hibernate.
- Application may depend on domain and ports, but not adapters or API runtime.
- Adapters may depend inward on application ports and domain.
- API packages assemble runtime concerns.
- Timeslot must not depend on platform domain, adapters, API runtime, repositories, entities, or
  persistence schema directly. The timeslot outbound platform adapter may depend on explicit
  `platform-exchange` APIs. Platform application services implement those APIs.
- Direct database access primitives must stay in outbound adapter packages. Production code should
  default to Spring Data JPA for owned persistence; native SQL is reserved for adapter-local
  database-specific behavior such as advisory locks.
- Preserve package-level JaCoCo gates for domain, application, adapters, API, and contracts after
  module collapse so module-level coverage gates do not hide weak package coverage.

## Alternatives

### Keep ADR-0001 Structure

Keep the current 11-module layout.

Benefits:

- Strong Gradle-level compile-time boundaries.
- Clear separation between domain, use cases, adapters, and runtime assembly.

Costs:

- More build metadata than the current product needs.
- Higher onboarding cost.
- Slower refactoring while the model is still in flux.

### Collapse Further To One Module

Use only the root project or one application module.

Benefits:

- Lowest Gradle overhead.

Costs:

- Bounded contexts become less visible.
- Architecture relies almost entirely on package discipline.

## Consequences

- `settings.gradle.kts` includes only `shared-kernel`, `platform`, and `timeslot`.
- Existing code moves into package-level layers inside `platform` and `timeslot`.
- JaCoCo coverage gates are enforced at both module and package level.
- ArchUnit tests become the primary layer enforcement mechanism.
- ADR-0001 remains as the historical implemented baseline.

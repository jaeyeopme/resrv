# ADR-0017: Collapse To Bounded-Context Gradle Modules

## Status

Accepted.

## Date

2026-05-25

## History

- Supersedes [ADR-0001](0001-bounded-context-module-baseline.md) as the active module decision.

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
platform.adapter.persistence
platform.adapter.web
platform.api

timeslot.domain
timeslot.application
timeslot.adapter.persistence
timeslot.adapter.web
timeslot.api
```

Use ArchUnit to enforce:

- Domain must not depend on application, adapters, API runtime, Spring, JPA, or Hibernate.
- Application may depend on domain and ports, but not adapters or API runtime.
- Adapters may depend inward on application ports and domain.
- API packages assemble runtime concerns.
- Timeslot must not depend on platform domain/application packages directly.

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
- JaCoCo coverage gates are recalibrated for the collapsed modules.
- ArchUnit tests become the primary layer enforcement mechanism.
- ADR-0001 remains as the historical implemented baseline.

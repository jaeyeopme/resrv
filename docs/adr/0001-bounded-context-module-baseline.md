# ADR-0001: Context-By-Layer Gradle Module Baseline

## Status

Superseded by [ADR-0017](0017-collapse-to-bounded-context-modules.md).

## Date

2026-05-25

## History

- `1ba6ff8 build: add bounded context modules`
- `9f5cdf2 build: harden new api modules`
- `37f8263 build: disable pending api bootRun tasks`
- Superseded by [ADR-0017](0017-collapse-to-bounded-context-modules.md) as the active module
  decision.

## Context

The redesign split the original tenant-local reservation API into platform and timeslot bounded
contexts. The first implementation used Gradle modules to encode both bounded context and
hexagonal layer boundaries.

Current branch baseline:

```text
shared-kernel
platform-domain
platform-application
platform-adapter-persistence
platform-adapter-web
platform-api
timeslot-domain
timeslot-application
timeslot-adapter-persistence
timeslot-adapter-web
timeslot-booking-api
```

Architecture tests were added with the module split to prevent domain and application dependency
leaks.

## Decision

Use this 11-module context-by-layer Gradle layout for the redesign implementation baseline:

This baseline proves the desired boundaries:

- Shared primitives live outside platform and timeslot.
- Platform and timeslot are separate contexts.
- Domain, application, adapter, and API runtime are separated.
- Timeslot does not depend on platform domain/application code.

## Alternatives

### Bounded-Context-Only Modules

Use:

```text
shared-kernel
platform
timeslot
```

This would keep bounded contexts as Gradle modules and enforce hexagonal layers with package
rules.

Benefits:

- Lower build and navigation overhead.
- Easier refactoring while the product model is still changing.
- Fewer Gradle dependencies and coverage gates.

## Consequences

- Gradle enforces layer boundaries before ArchUnit runs.
- Each layer has its own module configuration and coverage gate.
- Module count is higher than the current product scope may require.
- This decision can be superseded by a later ADR that collapses modules while preserving package
  boundaries.

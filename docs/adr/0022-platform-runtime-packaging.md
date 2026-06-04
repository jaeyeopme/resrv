# ADR-0022: Platform Runtime Packaging

## Status

Accepted

## Context

The implemented backend surface spans platform capabilities and booking
capabilities. `platform` is already the runnable Spring Boot API module, while
`timeslot` contains booking behavior with disabled `bootRun` and `bootJar`
tasks. Reviewers need one documented runtime that serves the implemented API
surface without introducing service-to-service transport, message brokers,
outbox processing, or a premature microservice split.

## Decision

Use the existing `platform` Spring Boot application as the canonical backend
runtime. The platform runtime depends on `timeslot`, scans both bounded-context
packages for controllers, services, repositories, entities, and migration
resources, and serves platform plus booking API groups from the same process.

`timeslot` remains a non-runnable bounded-context module. Its `bootRun` and
`bootJar` tasks stay disabled, and timeslot-to-platform decisions continue to
flow through the explicit `platform-exchange` APIs rather than platform
implementation packages.

Container packaging is owned by the platform runtime and uses Jib to build the
`resrv-platform-api:latest` image without adding a Dockerfile.

## Consequences

- Local review and early operation use one backend process: `:platform:bootRun`.
- Generated OpenAPI from the platform runtime is the current API contract
  surface for implemented platform and booking endpoint groups.
- Platform and timeslot Flyway migrations run from the same runtime classpath,
  while each bounded context keeps ownership of its own migration files.
- A future scale-driven service split remains possible, but requires a later
  ADR/spec for broker or HTTP transport, outbox, state propagation,
  observability, deployment, and failure handling.
- No payment, notification, UI, broker, outbox, projection storage, or
  independently deployed booking service is introduced by this decision.

# ADR-0020: Platform Exchange Boundary

## Status

Accepted.

## Date

2026-05-29

## Context

`timeslot` is traffic-sensitive and future domains such as ticketing may need the same extraction
path. The previous synchronous platform API surface lived inside the runnable `platform` module as
`platform.contract`, which forced consumers to compile against the platform implementation module
even when they only needed platform-owned lookup and decision APIs.

The product is still a modular monolith. Runtime process separation, HTTP/gRPC transport adapters,
Kafka/RabbitMQ, outbox tables, event schemas, and projections are intentionally deferred until a
separate runtime-split design exists.

## Decision

Add a dedicated `platform-exchange` Gradle module for platform-owned cross-context exchange APIs.
The module contains pure Java interfaces and DTO/value types only. It depends on `shared-kernel` and
must not contain Spring configuration, Spring stereotypes, persistence, transport adapters,
platform-backed implementations, or event packages.

`platform` depends on `platform-exchange` and implements those APIs with platform application
services. `timeslot` depends on `platform-exchange` and consumes the APIs only through its outbound
platform adapter. `timeslot` still does not read platform tables directly and still does not depend
on platform domain, adapters, API runtime, repositories, entities, or persistence schema.

`timeslot` `bootJar` and `bootRun` remain disabled in this decision.

## Consequences

- Cross-context synchronous lookup/check APIs are explicit without coupling consumers to the
  platform implementation module.
- Future traffic-sensitive domains can depend on `platform-exchange` without depending on
  `platform`.
- Architecture tests can enforce that the exchange module stays framework-free and event-free.
- A real runtime split still needs a later ADR/spec covering process boundaries, transport, outbox,
  message broker, event schema ownership, replay/backfill, and projection repair.

# Architecture decision records

This directory records technical decisions for `resrv`.

The ADR sequence is reconstructed from project git history. Each ADR lists the commits that
introduced or hardened the decision so future readers can inspect the implementation context.

## Index

| ADR | Status | Decision |
|---|---|---|
| [0001](0001-bounded-context-module-baseline.md) | Superseded | Context-by-layer Gradle module baseline |
| [0002](0002-shared-kernel-identity-primitives.md) | Accepted | Shared identity and timezone primitives |
| [0003](0003-platform-account-identity.md) | Accepted | Platform Account as the identity model |
| [0004](0004-business-and-membership-boundary.md) | Accepted | Business and BusinessMembership replace tenant-local admin boundary |
| [0005](0005-platform-persistence-schema.md) | Accepted | Platform schema owns account, business, and membership persistence |
| [0006](0006-account-scoped-jwt.md) | Accepted | Account-scoped JWTs with strict claim validation |
| [0007](0007-timeslot-booking-settings.md) | Accepted | Business booking settings with active business lookup |
| [0008](0008-reservable-resource-model.md) | Partially superseded | Reservable resources with optional booking overrides |
| [0009](0009-resource-schedule-model.md) | Accepted | Weekly schedules and date override schedules |
| [0010](0010-virtual-slots.md) | Accepted | Virtual slots selected by opaque slot IDs |
| [0011](0011-derived-reservation-state.md) | Accepted | Reservation state derived from timestamp facts |
| [0012](0012-reservation-persistence-and-locking.md) | Accepted | Fact-based reservation persistence and advisory lock port |
| [0013](0013-reservation-transition-authorization.md) | Accepted | Reservation transitions enforce owner or business membership access |
| [0014](0014-timeslot-booking-api-boundary.md) | Accepted | Timeslot booking API exposes booking workflow through platform lookup adapter |
| [0015](0015-replace-tenant-booking-api.md) | Accepted | Replace old tenant-local API with platform and timeslot redesign |
| [0016](0016-public-generated-openapi.md) | Accepted | Generated Swagger/OpenAPI docs remain publicly readable |
| [0017](0017-collapse-to-bounded-context-modules.md) | Accepted | Collapse layer modules into bounded-context Gradle modules |
| [0018](0018-account-security-hardening.md) | Accepted | Account security hardening with password reset recovery and active-state checks |
| [0019](0019-platform-contracts-for-timeslot-reads.md) | Accepted | Use explicit platform exchange APIs for timeslot business lookup, summary, and access decisions |
| [0020](0020-platform-exchange-boundary.md) | Accepted | Extract platform exchange APIs into a pure Java module |
| [0021](0021-staff-membership-administration.md) | Accepted | Staff membership current state with append-only access audit |
| [0022](0022-platform-runtime-packaging.md) | Accepted | Package platform as the canonical runtime for platform and booking APIs |
| [0023](0023-ticketing-bounded-context.md) | Accepted | Add ticketing as a bounded context assembled into the platform runtime |
| [0024](0024-timeslot-resource-id-only.md) | Accepted | Use resource IDs as the only timeslot resource identity |
| [0025](0025-selected-seat-ticket-purchase.md) | Accepted | Selected-seat ticket purchase confirmation claims seats on first success |
| [0026](0026-ticket-purchase-concurrency-idempotency.md) | Accepted | Harden ticket purchase confirmation with concurrency-safe claims and idempotency-key replay |

# Architecture Decision Records

This directory records durable technical decisions for `resrv`.

The ADR sequence is reconstructed from the redesign branch git history. Each ADR lists the commits
that introduced or hardened the decision so future readers can inspect the implementation context.

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
| [0008](0008-reservable-resource-model.md) | Accepted | Reservable resources with optional booking overrides |
| [0009](0009-resource-schedule-model.md) | Accepted | Weekly schedules and date override schedules |
| [0010](0010-virtual-slots.md) | Accepted | Virtual slots selected by opaque slot IDs |
| [0011](0011-derived-reservation-state.md) | Accepted | Reservation state derived from timestamp facts |
| [0012](0012-reservation-persistence-and-locking.md) | Accepted | Fact-based reservation persistence and advisory lock port |
| [0013](0013-reservation-transition-authorization.md) | Accepted | Reservation transitions enforce owner or business membership access |
| [0014](0014-timeslot-booking-api-boundary.md) | Accepted | Timeslot booking API exposes booking workflow through platform lookup adapter |
| [0015](0015-replace-tenant-booking-api.md) | Accepted | Replace old tenant-local API with platform and timeslot redesign |
| [0016](0016-public-generated-openapi.md) | Accepted | Generated Swagger/OpenAPI docs remain publicly readable |
| [0017](0017-collapse-to-bounded-context-modules.md) | Accepted | Collapse layer modules into bounded-context Gradle modules |

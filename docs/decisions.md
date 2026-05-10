# Decisions

This document summarizes durable architecture decisions. If more context is needed, inspect the git history, but the current working reference is this summary plus the implemented code.

## ADR-001: Shared database multi-tenancy

- Use one shared PostgreSQL database and isolate tenant-scoped data with `tenant_id`.
- Authenticated APIs use the JWT `tenantId`; public APIs resolve URL slug to a server-side `tenantId`.
- Tenant-scoped repositories require tenant id as an input.
- Unique constraints for tenant-scoped records must include the tenant scope.
- Tenant IDs in request bodies are not trusted.

## ADR-002: Public API tenant identification

- Public login/signup APIs identify the tenant with `/public/{tenantSlug}/...`.
- Slug is a public identifier and is treated as immutable in the MVP.
- The server is responsible for resolving slug to tenant id.

## ADR-003: Customer account + JWT reservation flow

- Creating, confirming, listing, and canceling reservations requires a logged-in customer JWT.
- Guest reservation tokens are not part of the MVP.
- Customer login is the minimum identity mechanism for check-in, cancellation, ownership checks, abuse prevention, and audit history.
- Customer email is unique within a tenant.
- Customer JWTs use the same token format as admin JWTs, with `role=CUSTOMER`.

## ADR-004: Reservation concurrency

- Overbooking is not handled by application duplicate checks alone.
- PostgreSQL partial exclusion constraints and `tstzrange` prevent active reservation time-range collisions.
- Active collision statuses are `HELD`, `CONFIRMED`, and `CHECKED_IN`.
- Reservation time ranges are interpreted as `[start, end)`.
- Expired-hold cleanup is a support mechanism; the database constraint is the core correctness boundary.

## ADR-005: TenantUser authentication

- Tenant users log in with email and password.
- The login API is `POST /public/{tenantSlug}/auth/login`.
- Passwords are verified with Argon2id.
- JWT claims include issuer, subject, audience, issued-at, expiration, jti, tenantId, userId, and role.
- Access token TTL is 30 minutes.
- Refresh tokens are outside the MVP scope.

## ADR-006: PostgreSQL JTI revocation blacklist

- Logout stores the current JWT `jti` in the PostgreSQL-backed `revoked_token` table.
- Revocation entries are considered active only until the JWT `exp`.
- Expired revocation rows are periodically deleted by a scheduled cleanup job; lookups also ignore expired rows.
- Revocation state is shared across application instances and survives application restarts.
- Redis was rejected for the current milestone because PostgreSQL/Flyway/Testcontainers already cover the needed durability and scale-out semantics without adding another infrastructure dependency.

## ADR-007: Resource identity and lifecycle

- Resource has both an internal UUID and a tenant-scoped slug.
- Resource slug is designed as a human-operable identifier.
- Resource deletion is modeled as `INACTIVE`, not hard delete.
- Resource capacity is fixed to 1 in the MVP; multiple capacity is represented by multiple resources.

## ADR-008: Availability precedence

- Date-specific Availability Exception takes precedence over weekly availability.
- `closed=true` exception creates no slots for that date.
- `closed=false` exception represents special hours for that date only.
- Weekly availability is a recurring rule based on Java `DayOfWeek`.

## ADR-009: Public API documentation

- Swagger UI, OpenAPI JSON, and OpenAPI YAML allow public access.
- Business APIs still follow Spring Security policies and JWT role guards.
- Swagger UI `Try it out` is disabled by default to avoid accidental mutating requests on a public review surface.

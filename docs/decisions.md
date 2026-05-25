# Decisions

This document summarizes durable architecture decisions. If more context is needed, inspect git
history, but the current working reference is this summary plus implemented code.

## ADR-001: Platform Account and Business Boundary

- Customer is now a platform `Account`.
- Business replaces Tenant in domain and API terminology.
- `BusinessMembership` grants `OWNER` or `STAFF` access to a Business.
- JWTs identify the account. They do not carry `businessId` or tenant-local role claims.
- Business authorization is resolved server-side from membership data.

## ADR-002: Split Deployables

- `platform-api` owns account registration, login, business creation, and membership ownership.
- `timeslot-booking-api` owns booking settings, resources, schedules, slots, and reservations.
- Timeslot code may read platform membership/business data through a narrow runtime adapter; it does
  not depend on platform domain/application code.

## ADR-003: Timeslot Booking Model

- Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
- Slots are virtual and selected by opaque `slotId`.
- Slot generation uses the business timezone.
- UTC instants are stored in persistence.

## ADR-004: Reservation Correctness

- A single resource/time range can have at most one active blocker.
- Hold creation uses PostgreSQL advisory transaction lock for the selected resource/start time.
- After locking, the service queries active blockers.
- Active blockers are confirmed reservations or holds whose `holdExpiresAt` is still in the future.
- Expired hold cleanup worker is not part of correctness.

## ADR-005: Reservation State

- Reservation state is derived from timestamp facts.
- `HELD` and `EXPIRED` are not persisted statuses.
- Confirm/release/customer cancel require reservation ownership.
- Business cancel/check-in/no-show require `OWNER` or `STAFF` membership.

## ADR-006: Public API Documentation

- Swagger UI, OpenAPI JSON, and OpenAPI YAML allow public access.
- Business mutation APIs still require JWT and application-level membership checks.

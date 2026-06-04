# ADR-0025: Selected-Seat Ticket Purchase Semantics

## Status

Accepted

Updated by [ADR-0026](0026-ticket-purchase-concurrency-idempotency.md), which adds selected-seat
oversell prevention and purchase-confirmation idempotency hardening.

## Context

Ticketing now needs a minimal selected-seat purchase lifecycle. The feature must prove ownership,
customer history, and business review without introducing payment, pre-purchase holds, checkout
attempts, failed-attempt storage, or high-contention hardening.

## Decision

Ticketing treats purchase confirmation as the first durable lifecycle action. A successful
confirmation creates one `TicketPurchase`, marks every selected `TicketSeat` as `PURCHASED`, and
records customer ownership. If any selected seat is missing, belongs to another event, or is already
purchased, the request fails without creating an attempt record.

Same-customer retries for the exact same purchased seat set return the existing purchase. Business
purchase activity is exposed only after server-side business access checks through
`platform-exchange`; unauthorized and missing event probes use the same public not-found style
response.

## Consequences

- No checkout, cancellation, expiration, or failed-attempt tables are introduced.
- Customer history and business activity are read projections over completed purchases only.
- High-contention oversell prevention and idempotency-key hardening were added later by ADR-0026.
- Generated OpenAPI from the platform runtime remains the endpoint contract.

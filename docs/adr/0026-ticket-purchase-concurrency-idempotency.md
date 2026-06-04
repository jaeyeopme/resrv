# ADR-0026: Ticket Purchase Concurrency And Idempotency

## Status

Accepted

## Context

Selected-seat ticket purchase confirmation already creates durable ownership as the first ticket
lifecycle action. The next risk is high-contention behavior: simultaneous customers can compete for
the same seats, and clients can repeat purchase confirmation requests after network or UI failures.
The system must prevent oversold seats and provide stable retry behavior without adding checkout
attempts, pre-purchase holds, payment state, or a failed-attempt ledger.

This decision covers transactional correctness under concurrent and repeated purchase confirmation
requests. It does not claim production peak-traffic readiness, load benchmarking, queueing,
backpressure, custom connection tuning, or horizontal throughput design.

## Decision

Ticketing requires a customer-provided idempotency key for purchase confirmation. The key is scoped
to the authenticated customer and bound to the ticket event plus canonical selected-seat set. The
same customer, key, event, and seat set replays the original purchased or unavailable public outcome
for 24 hours. Reusing the same key with different purchase details during the replay window is an
invalid retry. Reusing the same retained key after the replay window is an expired-key rejection.

Ticketing persists the minimal idempotency record needed to replay the original public outcome. It
does not create checkout, cancellation, expiration, payment, waitlist, queue, or general
failed-attempt resources. Expiry and cleanup eligibility are persisted time facts: expired records
remain available for expired-key rejection until they become cleanup-eligible 30 days after replay
expiry, and cleanup execution is not required for purchase correctness.

Seat claiming is coordinated in ticketing outbound persistence. The claim inserts the purchase,
locks and updates selected seats in deterministic order, and writes purchase-seat ownership only
when all selected seats are available. Losing concurrent confirmations return an unavailable-seats
outcome and do not create partial ownership.

## Consequences

- Exactly one purchase can own a contested selected seat.
- Multi-seat purchase confirmation remains all-or-nothing under contention.
- Customer retries can safely replay successful and losing outcomes without duplicating ownership.
- Idempotency keys do not authorize access across customers.
- Customer history and business activity remain projections over completed successful purchases.
- Generated OpenAPI and API integration tests cover the required idempotency key and new error
  distinctions.

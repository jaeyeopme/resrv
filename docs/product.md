# Product

## Problem

Small businesses often manage reservations across phone calls, messages, handwritten notes, spreadsheets, and vertical SaaS tools. That mix makes availability calculation, double-booking prevention, customer identity, and cancellation/change history fragile.

`resrv` provides an industry-neutral reservation API centered on **resource + time slot + customer**. The current goal is to prove that the backend can consistently answer: who reserved which resource, for which tenant, and for which time range.

## Product direction

- API-first reservation backend
- Independent tenant data boundary
- Industry-neutral Resource model
- Identity-based reservation flow for logged-in customers
- Overbooking prevention backed by PostgreSQL constraints
- Swagger/OpenAPI surface for external review

## Users

| User | Description | Current MVP capabilities |
|---|---|---|
| Tenant Admin | Business owner/operator | Create tenant, manage resources, configure availability, view reservations |
| Tenant Staff | Operations staff | Foundation for `STAFF` role access to operational APIs |
| Customer | Reservation customer | Register, log in, search slots, hold/confirm/cancel own reservations |
| Integrator | External UI/automation integrator | Read OpenAPI docs and integrate a client/frontend |

## Domain terms

| Term | Meaning |
|---|---|
| Tenant | Business or organization using the reservation service |
| TenantUser | Tenant administrator or staff account with `OWNER` or `STAFF` role |
| Customer | Reservation customer belonging to a tenant; reservation APIs require a customer JWT |
| Resource | Reservable item such as a seat, room, equipment item, or staff member |
| Weekly Availability | Recurring bookable hours by day of week |
| Availability Exception | Date-specific closure or special hours |
| Slot | Bookable time range calculated from tenant slot duration |
| Reservation | Customer-owned booking record |
| Hold | Temporary reservation state before confirmation; expires after hold TTL |

## MVP boundary

The current MVP provides this end-to-end flow:

1. A tenant signs up and creates the first administrator account.
2. The administrator logs in and creates a resource.
3. The administrator configures recurring hours and date-specific exceptions.
4. A customer registers and logs in.
5. The customer searches available slots.
6. The customer places a slot on hold.
7. A second active reservation for the same resource/time is blocked by database constraints.
8. The customer confirms the hold.
9. The customer lists and cancels their own reservations.
10. The administrator lists reservations for a resource.

## Business rules

- Only logged-in customers can create reservations. Guest reservation tokens are not part of the MVP.
- Customer login is the minimum identity mechanism for check-in, cancellation, abuse prevention, and audit history.
- Authenticated tenant boundaries come only from JWT `tenantId`.
- Public APIs that need tenant identification resolve URL `tenantSlug` on the server.
- Resource create/update request bodies do not accept tenant id.
- Resource deletion is modeled as `INACTIVE`, not hard delete.
- Default Resource capacity is 1. Multi-capacity is represented by multiple resources in the MVP.
- Time ranges use half-open intervals: `[start, end)`.
- Storage uses UTC `Instant`; availability calculation uses tenant timezone.
- Date-specific Availability Exception takes precedence over weekly rules.
- Reservation hold TTL and cancellation window follow tenant configuration.
- Overbooking prevention uses both application checks and a PostgreSQL exclusion constraint.
- Logout revocation is persisted in PostgreSQL so token revocation works across application instances.

## What is intentionally not in Phase 1

| Excluded item | Reason |
|---|---|
| Payments | Reservation correctness and authentication boundaries come first |
| Frontend | The API-first review surface comes first |
| Guest reservations | Identity-free reservations increase cancellation/check-in/abuse risk |
| Multi-resource capacity | Capacity 1 plus no-overbooking is proven first |
| Login rate limiting / lockout | Requires operations policy and storage choices; Phase 2 hardening |

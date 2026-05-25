# API

`resrv` now exposes two deployable API surfaces:

- `platform-api`: account, login, business, and business membership ownership.
- `timeslot-booking-api`: resource schedules, virtual slots, and reservations.

Customer is now a platform `Account`. Business replaces Tenant in domain and API terminology.
`BusinessMembership` grants `OWNER` or `STAFF` access to a Business.

## Authentication

Platform login returns an account-scoped JWT. The token identifies the account by `sub` and/or
`accountId`; it does not carry `businessId`, tenant-local roles, or booking context.

Timeslot mutation endpoints resolve authorization server-side:

- Settings, resource, and schedule writes require active `OWNER` or `STAFF` membership.
- Hold/confirm/release/customer cancel require the reservation owner account where applicable.
- Business cancel, check-in, and no-show require active `OWNER` or `STAFF` membership.

## Platform Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/accounts` | Register a platform account |
| `POST` | `/api/auth/login` | Issue an account JWT |
| `POST` | `/api/businesses` | Create a business and owner membership |

## Timeslot Endpoints

| Method | Path | Description |
|---|---|---|
| `PUT` | `/api/businesses/{businessId}/booking-settings` | Configure default booking settings |
| `POST` | `/api/businesses/{businessId}/resources` | Create a reservable resource |
| `GET` | `/api/businesses/{businessId}/resources` | List active resources |
| `PUT` | `/api/businesses/{businessId}/resources/{resourceId}/weekly-schedules/{dayOfWeek}` | Replace weekly schedule |
| `PUT` | `/api/businesses/{businessId}/resources/{resourceId}/date-schedule-overrides/{date}` | Replace date override |
| `GET` | `/api/businesses/{businessId}/resources/{resourceId}/slots?date=YYYY-MM-DD` | List virtual slots |
| `POST` | `/api/businesses/{businessId}/reservations` | Hold a slot by opaque `slotId` |
| `POST` | `/api/businesses/{businessId}/reservations/{reservationId}/confirm` | Confirm own active hold |
| `POST` | `/api/businesses/{businessId}/reservations/{reservationId}/release` | Release own active hold |
| `POST` | `/api/businesses/{businessId}/reservations/{reservationId}/cancel` | Cancel as customer or business |
| `POST` | `/api/businesses/{businessId}/reservations/{reservationId}/check-in` | Mark confirmed reservation checked in |
| `POST` | `/api/businesses/{businessId}/reservations/{reservationId}/no-show` | Mark confirmed reservation no-show |

## Reservation Model

Timeslot booking stores resources, schedules, booking settings, slots, and reservations.
Slots are virtual and selected by opaque `slotId`; slots are not persisted rows.

Reservation state is derived from timestamp facts. `HELD` and `EXPIRED` are not persisted
statuses. Expired hold cleanup worker is not part of correctness.

## Example Hold Request

```json
{
  "resourceId": "00000000-0000-0000-0000-000000000001",
  "slotId": "opaque-slot-id"
}
```

Slot and reservation response times are returned in the business timezone offset.

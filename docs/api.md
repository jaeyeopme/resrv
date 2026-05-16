# API

`resrv` exposes Springdoc OpenAPI and Swagger UI so reviewers can inspect the backend in an API-first workflow. Documentation endpoints are public, while business endpoints still follow their own authentication and role policies.

## API docs

| Document | URL | Description |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` | Browser-based API exploration |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | JSON for tooling integration |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` | Human-readable YAML |

Swagger UI `Try it out` is disabled by default. Mutating requests must be intentionally enabled before exposing a public review or demo environment.

The generated OpenAPI contract uses reviewer-facing tags, operation summaries, role-aware
Bearer requirements, success/error status codes, and field descriptions/examples. The
`OpenApiIntegrationTest` keeps those documentation affordances from regressing.

## Authentication model

| Principal | Login | JWT role | Main permissions |
|---|---|---|---|
| Tenant admin | `POST /public/{tenantSlug}/auth/login` | `OWNER` or `STAFF` | Manage resources/availability and view reservations per resource |
| Customer | `POST /public/{tenantSlug}/customers/login` | `CUSTOMER` | Search slots and hold/confirm/list/cancel own reservations |

JWTs include `jti`, `userId`, `tenantId`, `role`, `iss`, `aud`, and `exp` claims. Authenticated tenant boundaries come from the JWT `tenantId` claim, not from request bodies.

## Implemented endpoints

### Public onboarding and login

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/tenants` | Public | Create a tenant and first `OWNER` admin |
| `POST` | `/public/{tenantSlug}/auth/login` | Public | Tenant admin login |
| `POST` | `/public/{tenantSlug}/customers` | Public | Customer registration |
| `POST` | `/public/{tenantSlug}/customers/login` | Public | Customer login |

### Authenticated identity

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/logout` | Bearer JWT | Persistently revoke the current JWT JTI until expiration |
| `GET` | `/api/auth/me` | Bearer JWT | Return the current JWT `userId`, `tenantId`, and `role` |

### Admin resource, availability, and reservations

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/resources` | Admin JWT | Create a resource |
| `GET` | `/api/resources` | Admin JWT | List current tenant's `ACTIVE` resources |
| `GET` | `/api/resources/{resourceId}` | Admin JWT | Get one resource |
| `PUT` | `/api/resources/{resourceId}` | Admin JWT | Update resource name, slug, and description |
| `DELETE` | `/api/resources/{resourceId}` | Admin JWT | Deactivate a resource as `INACTIVE` |
| `PUT` | `/api/resources/{resourceId}/weekly-availability/{dayOfWeek}` | Admin JWT | Upsert recurring hours. `dayOfWeek` is Java `DayOfWeek` value `1`-`7` |
| `DELETE` | `/api/resources/{resourceId}/weekly-availability/{dayOfWeek}` | Admin JWT | Delete recurring hours |
| `PUT` | `/api/resources/{resourceId}/availability-exceptions/{date}` | Admin JWT | Upsert a date closure or special hours |
| `DELETE` | `/api/resources/{resourceId}/availability-exceptions/{date}` | Admin JWT | Delete a date exception |
| `GET` | `/api/resources/{resourceId}/reservations?date=YYYY-MM-DD` | Admin JWT | List reservations for one resource |
| `GET` | `/api/reservations?date=YYYY-MM-DD&resourceId=&customerId=&status=` | Admin JWT | Search tenant reservations for a bounded operator schedule view |
| `POST` | `/api/reservations/{reservationId}/admin-cancel` | Admin JWT | Cancel a held or confirmed reservation as an operator |
| `POST` | `/api/reservations/{reservationId}/check-in` | Admin JWT | Mark a confirmed reservation checked in at or after its start time |
| `POST` | `/api/reservations/{reservationId}/no-show` | Admin JWT | Mark a confirmed reservation no-show at or after its end time |

### Customer reservation

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/resources/{resourceId}/slots?date=YYYY-MM-DD` | Bearer JWT | List available slots for a date. Admins and customers can both query |
| `POST` | `/api/reservation-holds` | Customer JWT | Temporarily hold a slot as a customer |
| `POST` | `/api/reservation-holds/{reservationId}/confirm` | Customer JWT | Confirm the customer's own held reservation |
| `GET` | `/api/me/reservations` | Customer JWT | List the customer's own reservations |
| `POST` | `/api/me/reservations/{reservationId}/cancel` | Customer JWT | Cancel the customer's own reservation |

## Representative payloads

### Create tenant

```json
{
  "name": "Demo Studio",
  "slug": "demo-studio",
  "timezone": "Asia/Seoul",
  "slotDuration": 60,
  "holdTtl": 15,
  "cancellationWindow": 60,
  "admin": {
    "email": "owner@example.com",
    "password": "password123"
  }
}
```

### Create resource

```json
{
  "name": "Room A",
  "slug": "room-a",
  "description": "Consulting room"
}
```

### Weekly availability

```json
{
  "startTime": "09:00:00",
  "endTime": "18:00:00"
}
```

### Date availability exception

```json
{
  "closed": false,
  "startTime": "10:00:00",
  "endTime": "15:00:00"
}
```

A closed date is represented as:

```json
{
  "closed": true
}
```

### Hold reservation

```json
{
  "resourceId": "00000000-0000-0000-0000-000000000000",
  "startAt": "2026-05-11T00:00:00Z"
}
```

Reservation `status` can be `HELD`, `CONFIRMED`, `CUSTOMER_CANCELLED`,
`ADMIN_CANCELLED`, `CHECKED_IN`, `NO_SHOW`, or `EXPIRED`.
Admin operator transitions are intentionally bounded: admin-cancel accepts `HELD`
or `CONFIRMED`, check-in accepts `CONFIRMED` at/after `startAt`, and no-show
accepts `CONFIRMED` at/after `endAt`. `ADMIN_CANCELLED` and `NO_SHOW` release
the slot; `CHECKED_IN` remains an active occupancy state.

## Error model

API errors use Spring `ProblemDetail` responses.

| Situation | Typical status |
|---|---|
| Request validation failure or invalid path/query value | `400 Bad Request` |
| Authentication failure or invalid login body | `401 Unauthorized` |
| Role mismatch or not the owner of the reservation | `403 Forbidden` |
| Resource/customer/reservation not found in the current tenant | `404 Not Found` |
| Duplicate tenant/resource slug, duplicate customer email, slot collision, or invalid reservation transition | `409 Conflict` |

## Review scenario

A reviewer can verify the core backend quickly with this flow:

1. Inspect the full API surface in Swagger UI.
2. Create a tenant and administrator account.
3. Use the admin JWT to create a resource and configure availability.
4. Register a customer and obtain a customer JWT.
5. Use the customer JWT to list slots and create a reservation hold.
6. Confirm that a second hold for the same slot fails with `409 Conflict`.
7. Confirm the held reservation and verify the status in both customer and admin views.
8. Use the admin operator reservation search for the tenant-local date to inspect the day schedule.
9. For operator lifecycle verification, use separate confirmed reservations to test
   check-in or no-show when their time windows allow it.
10. Cancel a customer-owned held or confirmed reservation and confirm the slot
    becomes available again.

## Compact curl walkthrough

The commands below exercise the same happy path without depending on Swagger
mutating requests. They assume the app is running at `localhost:8080` and use
`jq` only to capture ids and tokens from responses. If you rerun the walkthrough
against the same database, change `TENANT_SLUG`, `ADMIN_EMAIL`, and
`CUSTOMER_EMAIL` first because tenant slugs and customer emails are unique.

```bash
BASE=http://localhost:8080
TENANT_SLUG=demo-studio
ADMIN_EMAIL=owner@example.com
CUSTOMER_EMAIL=customer@example.com
PASSWORD=password123
DEMO_DATE=2030-01-07 # Monday
DEMO_START=2030-01-07T09:00:00Z

curl -s -X POST "$BASE/api/tenants" \
  -H 'Content-Type: application/json' \
  -d "{
    \"name\":\"Demo Studio\",
    \"slug\":\"$TENANT_SLUG\",
    \"timezone\":\"UTC\",
    \"slotDuration\":60,
    \"holdTtl\":15,
    \"cancellationWindow\":60,
    \"admin\":{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$PASSWORD\"}
  }"

ADMIN_TOKEN=$(
  curl -s -X POST "$BASE/public/$TENANT_SLUG/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$PASSWORD\"}" |
    jq -r '.accessToken'
)

RESOURCE_ID=$(
  curl -s -X POST "$BASE/api/resources" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"name":"Room A","slug":"room-a","description":"Consulting room"}' |
    jq -r '.id'
)

curl -s -X PUT "$BASE/api/resources/$RESOURCE_ID/weekly-availability/1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"startTime":"09:00:00","endTime":"12:00:00"}'

curl -s -X POST "$BASE/public/$TENANT_SLUG/customers" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$CUSTOMER_EMAIL\",\"name\":\"Jane Customer\",\"password\":\"$PASSWORD\"}"

CUSTOMER_TOKEN=$(
  curl -s -X POST "$BASE/public/$TENANT_SLUG/customers/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"$PASSWORD\"}" |
    jq -r '.accessToken'
)

curl -s "$BASE/api/resources/$RESOURCE_ID/slots?date=$DEMO_DATE" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

RESERVATION_ID=$(
  curl -s -X POST "$BASE/api/reservation-holds" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"resourceId\":\"$RESOURCE_ID\",\"startAt\":\"$DEMO_START\"}" |
    jq -r '.id'
)

curl -s -X POST "$BASE/api/reservation-holds/$RESERVATION_ID/confirm" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

curl -s "$BASE/api/reservations?date=$DEMO_DATE&resourceId=$RESOURCE_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -s -X POST "$BASE/api/me/reservations/$RESERVATION_ID/cancel" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

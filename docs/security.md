# Security

## Security Goals

- Authenticate callers as platform accounts.
- Resolve business authorization on the server.
- Avoid trusting `businessId`, tenant role, or staff role claims from JWTs.
- Keep generated API documentation public while protecting application endpoints.
- Make deferred hardening explicit.

## JWT Model

JWTs are signed with HS256.

Required claims:

| Claim | Meaning |
|---|---|
| `sub` | Account UUID |
| `accountId` | Account UUID |
| `jti` | Token identifier |
| `iss` | Configured issuer |
| `aud` | Configured audience |
| `exp` | Expiration |

Platform and Timeslot validation require `sub` and `accountId` to be UUIDs, require `jti`, and
require `sub` to match `accountId`.

JWTs must not contain:

- `businessId`
- tenant-local role
- owner/staff authority
- customer/business actor authority

## Passwords

Passwords are hashed with Spring Security's Argon2 password encoder. Login uses a dummy hash path
for invalid inputs or missing accounts to reduce obvious timing differences.

## Public Endpoints

Generated documentation endpoints are public:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/v3/api-docs.yaml`

Platform public endpoints:

- `POST /api/accounts`
- `POST /api/auth/login`

Timeslot public read endpoints:

- `GET /api/businesses/*/resources`
- `GET /api/businesses/*/resources/*/slots`

## Business Authorization

Business write operations require active `BusinessMembership` with role `OWNER` or `STAFF`.

Timeslot checks membership through `BusinessAccessPort`, implemented by an outbound adapter that
calls an explicit `platform.contract` access check. Timeslot application code does not depend
directly on platform application services, domain, repositories, entities, or persistence schema.

## Reservation Authorization

Customer-scoped transitions require reservation ownership:

- Confirm own hold.
- Release own hold.
- Customer-cancel own confirmed reservation before cutoff.

Business-scoped transitions require owner/staff access:

- Business reservation list/search.
- Business cancel.
- Check-in.
- No-show.

## Data Boundary

Timeslot rows carry `business_id` and `customer_account_id` UUIDs. Business existence and active
status are resolved through platform lookup ports. The current migrations do not use cross-schema
foreign keys from timeslot to platform.

## Deferred Hardening

These are intentionally not implemented in the current phase:

- Login rate limiting.
- Failed-login lockout.
- Active hold quota per customer.
- Token revocation or logout blacklist for the redesigned account token.
- Full membership administration policy.
- Production CORS, CSRF, and deployment-specific network policy.

## Operational Requirements

- `RESRV_JWT_SECRET_KEY` must be at least 32 bytes.
- Do not use the documented development secret outside local development or tests.
- Use distinct issuer and audience values per environment.
- Do not put secrets in generated OpenAPI examples, docs, or logs.

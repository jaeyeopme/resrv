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

## Repeated Failed Sign-In Recovery

After 5 failed password sign-in attempts for the same account, the platform requires password reset
through the registered email address. The fifth failure sends a password reset email immediately.
Password sign-in remains blocked for that account until reset succeeds, even if a reset link expires
and must be reissued.

Security behavior:

- Sign-in responses remain non-enumerating for unknown accounts, bad passwords, and reset-required
  accounts.
- Raw passwords and raw reset tokens are never stored or logged.
- Account-scoped sign-in protection does not change business, resource, slot, or reservation
  availability for other accounts.
- Password failure hardening uses reset-required recovery as the account-level control.

Configuration:

| Property | Meaning |
|---|---|
| `spring.mail.*` | SMTP provider settings used by the password reset email adapter |
| `resrv.security.password-reset.public-base-url` | Public base URL used to build password reset links |
| `resrv.security.password-reset.token-ttl` | Password reset link lifetime |

## Public Endpoints

This section lists security exposure, not the full endpoint contract. Generated OpenAPI from the
platform runtime remains the canonical endpoint and schema contract.

Generated documentation endpoints are public:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/v3/api-docs.yaml`

Platform public endpoints:

- `POST /api/accounts`
- `POST /api/auth/login`
- `POST /api/auth/password-reset`

Timeslot public read endpoints:

- `GET /api/businesses/*/resources`
- `GET /api/businesses/*/resources/*/slots`
- `GET /api/public/businesses/*`
- `GET /api/public/businesses/*/resources`
- `GET /api/public/businesses/*/resources/*/slots`

Timeslot public booking hold endpoint:

- `POST /api/public/businesses/*/reservations`

The public booking flow uses the business slug only at the HTTP/API boundary. Public response bodies
and generated OpenAPI schemas must not expose the internal business UUID or customer account id.
After server-side slug resolution, timeslot may use the internal business UUID for owned data
relationships, slot binding, authorization, and persistence queries.

Public discovery must collapse syntactically valid missing, inactive, incomplete, and wrong-business
lookups into the same no-public-bookable-representation response. Non-sensitive denial facts may be
logged internally for debugging.

## Business Authorization

Business write operations require active `BusinessMembership` with role `OWNER` or `STAFF`.

Membership administration operations are owner-only:

- grant staff membership to an existing active account
- list current memberships
- list membership audit history
- change membership role
- disable membership

These operations resolve owner authority server-side from current membership state. Missing
businesses, wrong-business membership ids, and callers without owner authority return the same
not-found style public response. Last-owner downgrade or disable attempts are rejected.

Timeslot checks membership through `BusinessAccessPort`, implemented by an outbound adapter that
calls an explicit `platform-exchange` access check. Timeslot application code does not depend
directly on platform implementation services, domain, repositories, entities, or persistence schema.
Lookup contracts are separate from authorization contracts: `ActiveBusinessLookup` is active-only
availability data, while `BusinessSummaryLookup` may return inactive businesses for historical
customer-owned reservation rendering. Neither lookup should replace `BusinessAccessCheck` for
business-scoped authorization.

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

- Active hold quota per customer.
- Token revocation or logout blacklist for the redesigned account token.
- Production CORS, CSRF, and deployment-specific network policy.

## Operational Requirements

- `RESRV_JWT_SECRET_KEY` must be at least 32 bytes.
- Do not use the documented development secret outside local development or tests.
- Use distinct issuer and audience values per environment.
- Do not put secrets in generated OpenAPI examples, docs, or logs.

# Security

## Security goals

- Authenticate callers as platform accounts.
- Resolve business authorization on the server.
- Avoid trusting `businessId` or owner/staff/customer role claims from JWTs.
- Keep generated API documentation public while protecting application endpoints.
- Make deferred hardening explicit.

## JWT model

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
- business-local role
- owner/staff authority
- customer/business actor authority

## Passwords

Passwords are hashed with Spring Security's Argon2 password encoder. Login uses a dummy hash path
for invalid inputs or missing accounts to reduce obvious timing differences.

## Repeated failed sign-in recovery

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

## Public exposure

This section describes public exposure, not the full endpoint contract. Generated OpenAPI from the
platform runtime remains the canonical endpoint and schema contract.

Public surfaces:

- Generated API documentation: Swagger UI and `/v3/api-docs` JSON/YAML.
- Operational probes: Actuator health, liveness, and readiness. Probe responses must not expose
  secrets or private domain data.
- Platform account and authentication entry points: account registration, login, and password reset
  completion.
- Timeslot public booking flow: public business discovery, active resource discovery, generated slot
  discovery, and authenticated hold creation.

Ticketing protected endpoints are served by the platform runtime. Generated OpenAPI remains the
source of truth for exact paths and schemas; at a high level, ticketing exposes authenticated
selected-seat purchase confirmation, customer ticket history, and owner/staff business purchase
activity.

The public booking flow uses the business slug only at the HTTP/API boundary. Public response bodies
and generated OpenAPI schemas must not expose the internal business UUID or customer account id.
After server-side slug resolution, timeslot may use the internal business UUID for owned data
relationships, slot binding, authorization, and persistence queries.

Public discovery must collapse syntactically valid missing, inactive, incomplete, and wrong-business
lookups into the same no-public-bookable-representation response. Non-sensitive denial facts may be
logged internally for debugging.

Resource-scoped object probes also use a generic public not-found response when a valid resource id
is missing or belongs outside the addressed business. The request path may still identify the
attempted object; the problem detail must not reveal ownership or existence facts.

## Business authorization

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

## Reservation authorization

Customer-scoped transitions require reservation ownership:

- View own reservation detail.
- Confirm own hold.
- Release own hold.
- Customer-cancel own confirmed reservation before cutoff.

Customer reservation detail, confirm, release, and customer-cancel are IDOR-sensitive object probes.
Missing reservations and reservations owned by another customer return the same public `404` status
and detail. Internal logs may still distinguish missing from not-owned for support.

Business-scoped transitions require owner/staff access:

- Business reservation list/search.
- Business cancel.
- Check-in.
- No-show.

Business reservation operations keep business-membership authorization semantics. A caller without
active owner/staff access receives `403`; a reservation id outside the addressed business is treated
as not found for that route.

## Ticketing authorization

Ticket purchase confirmation requires an authenticated customer account. Unavailable selected seats
return `409 Conflict` without exposing the customer who owns the seats.

Customer ticket history is scoped to the authenticated customer and includes only completed
successful purchases for that account.

Business ticket activity requires active owner/staff access to the organizing business through
server-side membership checks. Missing ticket events and existing events outside caller authority
return the same not-found style public response. Internal handling may distinguish the causes, but
public problem details must not reveal whether an IDOR-sensitive ticket event exists.

Ticket purchase idempotency keys are customer-scoped. Invalid same-key retries and expired keys use
public problem reasons `invalid_retry` and `expired_key`. The replay window is 24 hours; expired
records remain retained until 30 days after replay expiry. Cleanup after retention must not be a
precondition for purchase correctness.

## Data boundary

Timeslot rows carry `business_id` and `customer_account_id` UUIDs. Ticketing rows carry
`business_id`, ticketing-owned ids, customer account ids, purchased seat ids, and customer-scoped
idempotency keys. Business existence, active status, and access decisions are resolved through
platform lookup or access ports. The current migrations do not use cross-schema foreign keys from
timeslot or ticketing to platform.

## Deferred hardening

These are not implemented in the current phase:

- Active hold quota per customer.
- Token revocation or logout blacklist for account-scoped JWTs.
- Production CORS, CSRF, and deployment-specific network policy.

## Operational requirements

- `RESRV_JWT_SECRET_KEY` must be at least 32 bytes.
- Do not use the documented development secret outside local development or tests.
- Use distinct issuer and audience values per environment.
- Do not put secrets in generated OpenAPI examples, docs, or logs.

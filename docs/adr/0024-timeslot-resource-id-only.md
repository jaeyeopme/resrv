# ADR-0024: Timeslot Resource ID-Only Identity

## Status

Accepted

## Context

ADR-0008 introduced business-scoped resource slugs as human-readable identifiers
for reservable resources. The product direction changed: timeslot resources do
not need a separate slug or handle because the database-backed `ResourceId` is
safe to expose and authorization is enforced server-side.

Keeping resource slug fields would preserve a second identity-like concept,
require duplicate slug validation, and keep public/API contracts tied to a value
that no longer has product meaning.

## Decision

Timeslot resource identity is the stable `ResourceId`. Resource names are
display/search metadata and are not unique within a business.

Remove resource slug semantics from:

- the timeslot resource domain model
- resource create and replace commands
- resource management request and response DTOs
- public resource discovery responses
- customer reservation resource summaries
- resource persistence mapping
- the `timeslot.resource` table

Business slug remains platform-owned and continues to scope public booking
discovery. Resource-scoped public discovery uses `businessSlug` plus
`resourceId`.

Obsolete `slug` or `handle` fields in resource create and replace request
bodies are invalid request fields. They are not ignored.

## Consequences

Duplicate or similar resource names under the same business are allowed.

Resource authorization remains server-side and IDOR-sensitive responses must not
depend on identifier secrecy.

Reservation rows remain tied to `resource_id`; no reservation rewrite is needed.

ADR-0008 remains historical context for the reservable resource model, but its
resource slug uniqueness and validation statements are superseded by this ADR.

# ADR-0008: Reservable Resource Model

## Status

Accepted.

## Date

2026-05-25

## History

- `8cf7fac feat(timeslot): add reservable resources`
- `c8bd47c fix(timeslot): tighten resource validation`
- `5f65794 fix(timeslot): validate resource description early`
- `1b0f181 refactor(timeslot): centralize slot policy`

## Context

Businesses need one or more reservable units. A resource can use business default booking settings
or override parts of the policy.

## Decision

Create `Resource` with:

- Business ID.
- Slug unique within business.
- Name.
- Optional description.
- Status.
- Optional booking overrides for slot duration, hold TTL, and cancellation window.

Validate resource fields at command/domain boundaries before persistence. Resource creation requires
business booking settings to exist so resource behavior has defaults.

Resolve final booking policy through `ResourceBookingOverrides.resolve(BusinessBookingSettings)`.
The resulting effective policy contains slot duration, hold TTL, cancellation window, and max
advance booking days. Null override fallback must stay inside the domain model instead of being
reimplemented by application services.

## Alternatives

### Store all booking policy only on resource

This gives maximum flexibility but duplicates common defaults across resources.

### Omit resource overrides

This is simpler, but many businesses need one resource to differ from the default schedule or policy.

## Consequences

- Resource defaults are inherited from business settings unless overridden.
- Resource slug uniqueness is business-scoped.
- Resource validation must reject invalid descriptions and malformed slugs before persistence.
- Application services consume an effective policy instead of duplicating nullable override
  fallback rules.

# ADR-0009: Resource Schedule Model

## Status

Accepted.

## Date

2026-05-25

## History

- `c1be43b feat(timeslot): support resource schedules`
- `1817609 feat(timeslot): persist booking configuration`

## Context

Slot generation needs business-local availability for each resource. The model must support normal
weekly hours and date-specific changes such as closures or special hours.

## Decision

Represent schedules as:

- Weekly resource schedules keyed by business, resource, and day of week.
- Date resource schedule overrides keyed by business, resource, and date.
- Schedule windows with local start/end times.

A date override replaces the weekly schedule for that date. An empty window list represents closed
availability for that day/date.

## Alternatives

### Persist Generated Availability Per Date

This makes slot lookup simple but creates storage churn and regeneration complexity when settings
change.

### Only Weekly Schedules

Weekly schedules are not enough for holidays, closures, or special operating days.

## Consequences

- Slot generation must check date override first, then weekly schedule.
- Schedules are stored as local times and interpreted using business timezone.
- Persistence owns window ordering and replacement behavior.


# ADR-0016: Public Generated OpenAPI

## Status

Accepted.

## Date

2026-05-25

## History

- `3ee8941 feat(auth): issue account scoped tokens`
- `195b6b5 feat(api): expose timeslot booking API`

## Context

This repository is meant to be reviewable. Reviewers should inspect the API surface without first
obtaining credentials. Public documentation must not imply public mutation access.

## Decision

Permit unauthenticated access to generated documentation endpoints:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/v3/api-docs.yaml`

Protect application endpoints under `/api/**` according to each API module's security rules.

## Alternatives

### Protect Swagger UI Behind Authentication

Appropriate for private production APIs, but it slows review and adds little value when source code
is available.

### Maintain Hand-Written OpenAPI As Source

Contract-first can be valid, but this project currently treats generated Springdoc output as the
contract surface.

## Consequences

- API docs can be reviewed without a token.
- Security-sensitive examples and secrets must never appear in generated docs.
- Public docs and public mutation access remain separate concerns.


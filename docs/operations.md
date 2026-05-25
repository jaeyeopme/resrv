# Operations

## Local Prerequisites

- JDK 25+
- Node.js 24+ for repository tooling
- Docker running for PostgreSQL and Testcontainers

## Repository Tooling

```bash
npm ci
npm run hooks:install
```

Commit messages are validated by commitlint through Lefthook and CI.

## Quality Check

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

`rewriteDryRun` previews OpenRewrite cleanup recipes without changing files.

## Local Database

The root `compose.yml` defines PostgreSQL 16:

```text
POSTGRES_DB=resrv
POSTGRES_USER=resrv
POSTGRES_PASSWORD=secret
```

Spring Boot Docker Compose support can start this service for local `bootRun` when the boot task
runs from the repository root.

## Platform API Local Run

```bash
RESRV_JWT_SECRET_KEY=01234567890123456789012345678901 \
RESRV_JWT_ISSUER=resrv-dev \
RESRV_JWT_AUDIENCE=resrv-api \
RESRV_JWT_EXPIRATION=3600 \
./gradlew :platform:bootRun
```

Open:

- <http://localhost:8080/swagger-ui.html>
- <http://localhost:8080/v3/api-docs>
- <http://localhost:8080/v3/api-docs.yaml>

## Timeslot API Local Run

`timeslot` currently has `bootJar` and `bootRun` disabled. This is intentional pending the final
timeslot runtime packaging decision recorded in ADR-0014.

Until that decision is made, timeslot API behavior is verified through integration tests:

```bash
./gradlew :timeslot:test
```

## Required JWT Configuration

| Property | Environment variable | Meaning |
|---|---|---|
| `resrv.jwt.secret-key` | `RESRV_JWT_SECRET_KEY` | HS256 signing/verification key, at least 32 bytes |
| `resrv.jwt.issuer` | `RESRV_JWT_ISSUER` | Expected token issuer |
| `resrv.jwt.audience` | `RESRV_JWT_AUDIENCE` | Expected token audience |
| `resrv.jwt.expiration` | `RESRV_JWT_EXPIRATION` | Token lifetime in seconds |

## Migrations

Flyway migrations are stored in bounded-context modules:

| Migration | Purpose |
|---|---|
| `platform/src/main/resources/db/migration/V9__create_platform_schema.sql` | Platform account, business, membership |
| `timeslot/src/main/resources/db/migration/V10__create_timeslot_schema.sql` | Timeslot settings, resources, schedules, reservations |

## Troubleshooting

| Symptom | Check |
|---|---|
| Tests fail before connecting to PostgreSQL | Docker is running |
| JWT config validation fails | Secret is at least 32 bytes and issuer/audience/expiration are set |
| `timeslot:bootRun` is unavailable | Boot task is currently disabled by design |
| Swagger returns 401 for docs | Security config should permit `/swagger-ui/**` and `/v3/api-docs/**` |
| Coverage verification fails | Inspect module JaCoCo HTML report under `<module>/build/reports/jacoco/test/html` |

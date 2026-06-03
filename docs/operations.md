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

`platform` is the canonical backend runtime. It serves platform, booking, and ticketing API groups
from one process.

```bash
./gradlew :platform:bootRun
```

The Gradle `bootRun` task uses the `local` profile when no active profile is
set. That profile contains development-only JWT defaults. Do not use the
development defaults outside local `bootRun`.

Open:

- <http://localhost:8080/swagger-ui.html>
- <http://localhost:8080/v3/api-docs>
- <http://localhost:8080/v3/api-docs.yaml>
- <http://localhost:8080/actuator/health/liveness>
- <http://localhost:8080/actuator/health/readiness>

The local run path may use Spring Boot Docker Compose support to discover the root `compose.yml`.

## Platform API Production-Like Run

Use the `prod` profile when checking deployed-environment configuration. This profile disables local
Docker Compose discovery and requires explicit datasource, JWT, and password reset settings from the
environment.

```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.internal:5432/resrv \
SPRING_DATASOURCE_USERNAME=resrv \
SPRING_DATASOURCE_PASSWORD=<secret> \
RESRV_JWT_SECRET_KEY=<at-least-32-bytes> \
RESRV_JWT_ISSUER=resrv-prod \
RESRV_JWT_AUDIENCE=resrv-api \
RESRV_JWT_EXPIRATION=3600 \
RESRV_SECURITY_PASSWORD_RESET_PUBLIC_BASE_URL=https://app.example.com \
RESRV_SECURITY_PASSWORD_RESET_TOKEN_TTL=PT30M \
java -jar platform/build/libs/resrv-platform-api-0.0.1-SNAPSHOT.jar
```

Do not use the sample local JWT secret or local PostgreSQL password in deployed environments.
Missing mandatory `prod` settings fail startup through configuration binding or dependency
initialization.

## Timeslot API Local Run

`timeslot` currently has `bootJar` and `bootRun` disabled. This is intentional:
ADR-0022 packages booking APIs into the platform runtime instead of creating a separate timeslot
runtime.

Until a later runtime-split and outbox/message-broker decision is made, standalone timeslot runtime
execution is not a supported operation. Timeslot API behavior is verified through integration tests:

```bash
./gradlew :timeslot:test
```

## Ticketing API Local Run

Ticketing is assembled into the platform runtime. There is no separate ticketing backend runtime,
`bootRun`, health endpoint, deployment unit, or service-to-service API in the current architecture.

Use the platform runtime for ticketing API probes:

```bash
./gradlew :platform:bootRun
```

Endpoint and schema details come from generated OpenAPI at `/v3/api-docs` and
`/v3/api-docs.yaml`; this document intentionally does not duplicate a ticket endpoint catalog.

Ticket purchase confirmation idempotency replays the original public outcome for 24 hours. Expired
idempotency records remain eligible for retention until 30 days after replay expiry. Cleanup may
delete retained expired records after that point, but purchase correctness does not depend on
cleanup.

## Runtime Packaging

Build the executable platform runtime artifact:

```bash
./gradlew :platform:bootJar
```

Build the local Jib image:

```bash
./gradlew :platform:jibDockerBuild
```

The local image name is `resrv-platform-api:latest`.

Smoke-check a running packaged backend:

```bash
curl -fsS http://localhost:8080/actuator/health/liveness
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/v3/api-docs >/tmp/resrv-api.json
```

The health endpoints expose component status only and never expose secrets, credentials, account
data, business data, or reservation data.

## Required JWT Configuration

| Property | Environment variable | Meaning |
|---|---|---|
| `resrv.jwt.secret-key` | `RESRV_JWT_SECRET_KEY` | HS256 signing/verification key, at least 32 bytes |
| `resrv.jwt.issuer` | `RESRV_JWT_ISSUER` | Expected token issuer |
| `resrv.jwt.audience` | `RESRV_JWT_AUDIENCE` | Expected token audience |
| `resrv.jwt.expiration` | `RESRV_JWT_EXPIRATION` | Token lifetime in seconds |

## Password Reset Email Configuration

Password reset delivery uses Spring Mail. Configure the standard `spring.mail.*` properties for the
SMTP provider used by the environment.

| Property | Environment variable | Meaning |
|---|---|---|
| `spring.mail.host` | `SPRING_MAIL_HOST` | SMTP host |
| `spring.mail.port` | `SPRING_MAIL_PORT` | SMTP port |
| `spring.mail.username` | `SPRING_MAIL_USERNAME` | SMTP username when required |
| `spring.mail.password` | `SPRING_MAIL_PASSWORD` | SMTP password when required |
| `spring.mail.properties.mail.smtp.auth` | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | Whether SMTP auth is enabled |
| `spring.mail.properties.mail.smtp.starttls.enable` | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | Whether STARTTLS is enabled |
| `resrv.security.password-reset.public-base-url` | `RESRV_SECURITY_PASSWORD_RESET_PUBLIC_BASE_URL` | Public base URL used when building password reset links |
| `resrv.security.password-reset.token-ttl` | `RESRV_SECURITY_PASSWORD_RESET_TOKEN_TTL` | Password reset link lifetime, expressed as a duration |

Tests should replace SMTP with a fake adapter and must not contact an external email provider.

## Migrations

Flyway migrations are stored in bounded-context modules:

| Migration | Purpose |
|---|---|
| `platform/src/main/resources/db/migration/V9__create_platform_schema.sql` | Platform account, business, membership |
| `timeslot/src/main/resources/db/migration/V10__create_timeslot_schema.sql` | Timeslot settings, resources, schedules, reservations |
| `platform/src/main/resources/db/migration/V11__account_security_hardening.sql` | Sign-in protection and password reset recovery |
| `platform/src/main/resources/db/migration/V12__staff_membership_management.sql` | Membership timestamps and access audit history |
| `ticketing/src/main/resources/db/migration/V20__create_ticketing_schema.sql` | Ticketing event and inventory baseline |
| `timeslot/src/main/resources/db/migration/V21__remove_timeslot_resource_slug.sql` | Timeslot resource slug removal |

The platform runtime loads `classpath:db/migration`, so platform, timeslot, and ticketing migration
resources on the runtime classpath are applied through the same startup path. Ticketing migrations
therefore participate in platform runtime startup and checks; there is no separate ticketing
migration command for normal local operation.

Migration success can be checked through startup logs and the `flyway_schema_history` table. The
platform runtime is not ready for traffic when the database is unavailable or required migrations
cannot complete.

## Health And Readiness

Only health endpoints are exposed through Actuator:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

Liveness reports whether the process is alive. Readiness includes database availability and should
be used before sending traffic to the backend. Generated OpenAPI remains the application API
contract; health endpoints are operational probes, not an endpoint catalog.

## Troubleshooting

| Symptom | Check |
|---|---|
| Tests fail before connecting to PostgreSQL | Docker is running |
| JWT config validation fails | Secret is at least 32 bytes and issuer/audience/expiration are set |
| Readiness is down | Check PostgreSQL connectivity and migration success |
| `timeslot:bootRun` is unavailable | Boot task is disabled by design; run `:platform:bootRun` |
| Ticketing runtime command is unclear | Ticketing is served by `:platform:bootRun`; there is no standalone ticketing runtime |
| Swagger returns 401 for docs | Security config should permit `/swagger-ui/**` and `/v3/api-docs/**` |
| Coverage verification fails | Inspect module JaCoCo HTML report under `<module>/build/reports/jacoco/test/html` |

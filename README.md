# resrv — Multi-tenant B2B Reservation API

`resrv`는 사업자가 예약 가능한 자원(Resource)을 운영하고, 고객이 로그인 후 가능한 시간대를 조회해 예약을 hold/confirm/cancel 할 수 있는 **멀티테넌트 B2B 예약 API**입니다.

포트폴리오 관점의 핵심은 단순 CRUD가 아니라 **테넌트 격리, 역할 기반 인증, 예약 가능 시간 계산, hold 기반 예약 생명주기, PostgreSQL 동시성 제약, OpenAPI 문서화, Testcontainers 통합 테스트**가 하나의 백엔드 시스템으로 연결되어 있다는 점입니다.

## What is implemented now

| 영역 | 구현된 기능 |
|---|---|
| Tenant onboarding | 테넌트 생성과 최초 `OWNER` 관리자 등록 |
| Auth | 관리자 로그인, 고객 로그인, JWT 발급, logout JTI blacklist, `/me` |
| Resource management | 테넌트 범위 Resource 생성/조회/수정/비활성화 |
| Availability | 요일별 반복 운영 시간, 날짜별 휴무/특별 운영 시간 |
| Slot search | 테넌트 timezone과 slot duration 기준의 예약 가능 slot 조회 |
| Reservation lifecycle | 고객 로그인 기반 hold → confirm → customer cancel |
| Admin audit | 관리자용 Resource별 예약 조회 |
| No overbooking | PostgreSQL `EXCLUDE USING gist` 제약으로 활성 예약 시간대 충돌 방지 |
| API docs | Springdoc OpenAPI JSON/YAML과 Swagger UI 공개 |

## Quick links

| 보고 싶은 것 | 위치 |
|---|---|
| API surface / Swagger | [`docs/api.md`](docs/api.md), `/swagger-ui.html`, `/v3/api-docs`, `/v3/api-docs.yaml` |
| 제품 의도와 MVP 경계 | [`docs/product.md`](docs/product.md) |
| 아키텍처와 모듈 경계 | [`docs/architecture.md`](docs/architecture.md) |
| 현재 상태와 다음 단계 | [`docs/roadmap.md`](docs/roadmap.md) |
| 결정 기록 | [`docs/decisions.md`](docs/decisions.md) |
| 내부 실행 위키 | [`omx_wiki/`](omx_wiki/README.md) |

## Architecture at a glance

```text
adapter-web         adapter-persistence
     \                  /
      \                /
        application
            |
          domain

bootstrap: Spring Boot assembly, Security/JWT/OpenAPI, integration tests
```

- `domain`: Spring/JPA에 의존하지 않는 도메인 모델과 불변 조건
- `application`: 유스케이스와 port 인터페이스, 트랜잭션 경계
- `adapter-web`: REST controller, DTO, validation, 인증 principal 변환
- `adapter-persistence`: JPA, Flyway migration, PostgreSQL 제약
- `bootstrap`: 실행 조립, Security/JWT/OpenAPI 설정, 통합 테스트

의존 방향은 `adapter-* -> application -> domain`입니다. 자세한 내용은 [`docs/architecture.md`](docs/architecture.md)를 기준으로 합니다.

## Tech stack

| 분류 | 기술 |
|---|---|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4, Spring MVC, Spring Security |
| API docs | Springdoc OpenAPI, Swagger UI |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA |
| Security | JWT HS256, Argon2id password hashing, Caffeine JTI blacklist |
| Build / quality | Gradle 9, Spotless, Checkstyle, JaCoCo, ArchUnit |
| Tests | JUnit 5, Testcontainers |

## Run locally

### Prerequisites

- JDK 25+
- Docker running for PostgreSQL/Testcontainers

### Start the API

```bash
./gradlew :bootstrap:bootRun
```

`bootRun` starts PostgreSQL through the root `compose.yml` and uses a built-in
development JWT secret so the API can be reviewed without extra setup. Set
`JWT_SECRET_KEY` to a 32+ byte value for any shared, staged, or production-like
environment.

Keep the terminal open while reviewing the API. The startup is complete when the
log prints `Started ResrvApplication`; because `bootRun` is a long-running
server task, it does not return `BUILD SUCCESSFUL` until the process exits. If
you stop it with `Ctrl-C` or a forced kill, Gradle may report a non-zero exit
such as `130` or `143` even though startup already succeeded.

Then open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

Swagger UI is public for review, but mutating `Try it out` is disabled by default in `application.yml`.

## Main API flow

1. `POST /api/tenants` — create a tenant and first `OWNER` admin.
2. `POST /public/{tenantSlug}/auth/login` — login as admin and receive a Bearer token.
3. `POST /api/resources` — create a reservable resource.
4. `PUT /api/resources/{resourceId}/weekly-availability/{dayOfWeek}` — set weekly hours.
5. `POST /public/{tenantSlug}/customers` — register a customer.
6. `POST /public/{tenantSlug}/customers/login` — login as customer.
7. `GET /api/resources/{resourceId}/slots?date=YYYY-MM-DD` — list available slots.
8. `POST /api/reservation-holds` — hold a slot as the logged-in customer.
9. `POST /api/reservation-holds/{reservationId}/confirm` — confirm the hold.
10. `POST /api/me/reservations/{reservationId}/cancel` — cancel a customer-owned reservation.

Detailed endpoint and payload notes are in [`docs/api.md`](docs/api.md).

## Verification

```bash
./gradlew spotlessApply
./gradlew check
```

`check` runs compilation, unit/slice/integration tests, Checkstyle, ArchUnit, JaCoCo coverage verification/report generation, and Testcontainers-backed checks. Docker must be running for the Testcontainers portion.

## Inspection checklist

- Start from Swagger UI and verify the API surface is discoverable.
- Read [`docs/product.md`](docs/product.md) to understand why customer login is mandatory for reservations.
- Inspect [`docs/architecture.md`](docs/architecture.md) for hexagonal boundaries and ArchUnit enforcement.
- Inspect `adapter-persistence/src/main/resources/db/migration/V7__create_reservation.sql` for DB-level no-overbooking.
- Inspect `bootstrap/src/test/java/io/resrv/bootstrap/ReservationMvpIntegrationTest.java` for the end-to-end reservation scenario.

## Explicitly deferred

The following are intentionally deferred hardening items and should not be interpreted as missing accidentals: login rate limiting, failed-login lockout, tenant/admin active-state validation filter, and persistent DB/Redis JTI blacklist. See [`docs/roadmap.md`](docs/roadmap.md).

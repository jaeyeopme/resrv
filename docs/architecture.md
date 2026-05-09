# Architecture

## Overview

`resrv`는 Hexagonal Architecture(Ports & Adapters)를 따르는 5개 Gradle subproject 기반 Spring Boot 4 애플리케이션입니다.

```text
adapter-web         adapter-persistence
     \                  /
      \                /
        application
            |
          domain

bootstrap: 실행 조립, Security/JWT/Password/OpenAPI 어댑터, 통합 테스트
```

## Subproject responsibilities

| Subproject | 책임 | 금지/주의 |
|---|---|---|
| `domain` | 도메인 모델, 값 객체, 불변 조건 | Spring/JPA 의존 금지 |
| `application` | 유스케이스, 입력/출력 포트, 트랜잭션 경계 | 웹/JPA DTO 누수 금지 |
| `adapter-web` | REST 요청/응답, validation, 인증 principal 변환 | 비즈니스 규칙 직접 구현 금지 |
| `adapter-persistence` | JPA 엔티티, repository, mapper, Flyway migration | 웹 계층 의존 금지 |
| `bootstrap` | 애플리케이션 조립, Security/JWT/Password 구현, OpenAPI 설정, 통합 테스트 | 도메인 규칙을 설정 코드에 숨기지 않기 |

## Dependency direction

- `domain`은 순수 Java 도메인 모델입니다.
- `application`은 `domain`을 사용하고 port를 정의합니다.
- `adapter-web`과 `adapter-persistence`는 `application` port에 붙는 adapter입니다.
- `bootstrap`은 Spring Boot 실행과 테스트 조립을 담당합니다.
- ArchUnit으로 의존 방향을 지속적으로 검증합니다.

## Multi-tenancy

- 데이터베이스는 shared database 모델을 사용합니다.
- 테넌트 스코프 테이블은 `tenant_id`를 기준으로 격리합니다.
- 인증된 API는 JWT claim의 `tenantId`를 서버 측 테넌트 경계로 사용합니다.
- public login/signup API는 URL의 `tenantSlug`를 서버에서 `tenantId`로 해석합니다.
- 클라이언트가 request body로 넘기는 tenant id는 신뢰하지 않습니다.
- 테넌트 범위 unique constraint는 반드시 `tenant_id`를 포함합니다.

## Authentication and authorization boundary

| API group | Tenant source | Auth | Role boundary |
|---|---|---|---|
| `POST /api/tenants` | 신규 생성 | Public | 없음 |
| `/public/{tenantSlug}/auth/login` | URL slug | Public credential | Tenant admin 로그인 |
| `/public/{tenantSlug}/customers` | URL slug | Public credential | Customer 가입/로그인 |
| `/api/auth/**` | JWT `tenantId` | Bearer JWT | 인증 사용자 |
| Resource CRUD | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |
| Availability write | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |
| Slot search | JWT `tenantId` | Bearer JWT | 관리자 또는 고객 |
| Customer reservation | JWT `tenantId` | Bearer JWT | `CUSTOMER` |
| Admin reservation audit | JWT `tenantId` | Bearer JWT | `OWNER`/`STAFF` |

JWT는 자체 발급 HS256 토큰입니다. access token TTL은 30분이며 refresh token은 MVP 범위가 아닙니다. 로그아웃은 Phase 1에서 Caffeine 기반 JTI blacklist로 처리합니다.

## Data model

Flyway migration 기준 현재 스키마는 다음을 포함합니다.

| 테이블 | 핵심 필드 / 제약 |
|---|---|
| `tenant` | slug, timezone, slot duration, hold TTL, cancellation window, status |
| `admin` | tenant_id, email, hashed_password, role, active |
| `resource` | tenant_id, slug, name, description, status, created_at, updated_at |
| `customer` | tenant_id, email, name, hashed_password, active, created_at |
| `resource_weekly_availability` | tenant_id, resource_id, day_of_week, start_time, end_time |
| `resource_availability_exception` | tenant_id, resource_id, date, closed, optional start/end time |
| `reservation` | tenant_id, resource_id, customer_id, start_at, end_at, status, hold/confirm/cancel timestamps |

중요 제약:

- tenant slug는 전역 unique입니다.
- admin email, customer email, resource slug는 tenant 범위 unique입니다.
- Resource status는 `ACTIVE`, `INACTIVE`입니다.
- Reservation status는 `HELD`, `CONFIRMED`, `CUSTOMER_CANCELLED`, `ADMIN_CANCELLED`, `CHECKED_IN`, `NO_SHOW`, `EXPIRED`입니다.
- `reservation`은 PostgreSQL `btree_gist`와 `tstzrange(start_at, end_at, '[)')` 기반 exclusion constraint로 활성 예약 overlap을 막습니다.

## Reservation correctness

예약 correctness는 세 단계로 구성됩니다.

1. **Availability calculation**: 날짜별 exception을 먼저 보고, 없으면 weekly rule을 사용합니다. 테넌트 timezone에서 local date/time을 계산하고 UTC `Instant` slot으로 변환합니다.
2. **Application guard**: hold 생성 전 활성 overlap을 조회하고 예약 가능한 slot인지 확인합니다. 만료된 hold는 주요 reservation workflow 진입 시 `EXPIRED`로 정리합니다.
3. **Database guard**: 동시 요청이 application guard를 동시에 통과해도 PostgreSQL exclusion constraint가 `HELD`, `CONFIRMED`, `CHECKED_IN` overlap을 막습니다.

예약 시간 구간은 half-open interval(`[start, end)`)로 해석합니다.

## OpenAPI and public review surface

- Springdoc OpenAPI가 `/v3/api-docs`와 `/v3/api-docs.yaml`을 제공합니다.
- Swagger UI는 `/swagger-ui.html`에서 접근 가능합니다.
- OpenAPI 문서 엔드포인트는 공개 접근을 허용합니다.
- Swagger UI의 mutating `Try it out`은 기본 비활성화되어 있습니다.

## Test strategy

| 레벨 | 위치 | 목적 |
|---|---|---|
| Domain tests | `domain/src/test` | 값 객체/도메인 불변식 검증 |
| Application tests | `application/src/test` | 유스케이스와 port 상호작용 검증 |
| Web adapter tests | `adapter-web/src/test` | REST 계약, validation, security boundary 검증 |
| Persistence tests | `adapter-persistence/src/test` | JPA/Flyway/PostgreSQL 연동 검증 |
| Integration tests | `bootstrap/src/test` | 실제 조립 후 주요 flow 검증 |
| Architecture tests | `bootstrap/src/test/.../architecture` | 모듈 의존 방향과 계층 규칙 검증 |

대표 통합 테스트:

- `ResourceManagementIntegrationTest`: 관리자 로그인과 Resource CRUD flow
- `ReservationMvpIntegrationTest`: 고객 가입/로그인, availability, slot, hold/confirm/cancel, no-overbooking flow
- `OpenApiIntegrationTest`: OpenAPI/Swagger 공개성과 현재 API path 노출 여부

## Explicitly deferred hardening

다음 보안/운영 강화는 Phase 2로 미뤄져 있으며, 별도 요청 없이 구현하지 않습니다.

| ID | 내용 |
|---|---|
| T100 | 로그인 rate limiting |
| T101 | 로그인 실패 횟수/잠금 |
| T102 | `UserStateValidationFilter`로 테넌트/관리자 활성 상태 강제 |
| T103 | DB/Redis 기반 persistent JTI blacklist |

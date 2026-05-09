# API

`resrv`는 OpenAPI-first에 가깝게 리뷰할 수 있도록 Springdoc OpenAPI와 Swagger UI를 제공합니다. 문서 엔드포인트는 공개되어 있지만, 실제 비즈니스 API는 각 엔드포인트의 인증/역할 정책을 따릅니다.

## API docs

| 문서 | URL | 설명 |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` | 브라우저 기반 API 탐색 |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | 도구 연동용 JSON |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` | 사람이 읽기 좋은 YAML |

Swagger UI의 `Try it out`은 기본 비활성화되어 있습니다. 공개 포트폴리오 또는 데모 환경에 노출할 때도 mutating 요청을 의도적으로 켜야 합니다.

## Authentication model

| Principal | 로그인 | JWT role | 주요 권한 |
|---|---|---|---|
| Tenant admin | `POST /public/{tenantSlug}/auth/login` | `OWNER` 또는 `STAFF` | Resource/Availability 관리, Resource별 예약 조회 |
| Customer | `POST /public/{tenantSlug}/customers/login` | `CUSTOMER` | Slot 조회, 본인 예약 hold/confirm/list/cancel |

JWT에는 `jti`, `userId`, `tenantId`, `role`, `iss`, `aud`, `exp` claim이 포함됩니다. 인증된 API의 테넌트 경계는 request body가 아니라 JWT의 `tenantId` claim에서만 가져옵니다.

## Implemented endpoints

### Public onboarding and login

| Method | Path | Auth | 설명 |
|---|---|---|---|
| `POST` | `/api/tenants` | Public | 테넌트와 최초 `OWNER` 관리자 생성 |
| `POST` | `/public/{tenantSlug}/auth/login` | Public | Tenant admin 로그인 |
| `POST` | `/public/{tenantSlug}/customers` | Public | 고객 회원가입 |
| `POST` | `/public/{tenantSlug}/customers/login` | Public | 고객 로그인 |

### Authenticated identity

| Method | Path | Auth | 설명 |
|---|---|---|---|
| `POST` | `/api/auth/logout` | Bearer JWT | 현재 JWT의 JTI를 만료 시점까지 무효화 |
| `GET` | `/api/auth/me` | Bearer JWT | 현재 JWT의 `userId`, `tenantId`, `role` 조회 |

### Admin resource and availability

| Method | Path | Auth | 설명 |
|---|---|---|---|
| `POST` | `/api/resources` | Admin JWT | Resource 생성 |
| `GET` | `/api/resources` | Admin JWT | 현재 테넌트의 `ACTIVE` Resource 목록 |
| `GET` | `/api/resources/{resourceId}` | Admin JWT | Resource 단건 조회 |
| `PUT` | `/api/resources/{resourceId}` | Admin JWT | Resource 이름/slug/설명 수정 |
| `DELETE` | `/api/resources/{resourceId}` | Admin JWT | Resource를 `INACTIVE`로 비활성화 |
| `PUT` | `/api/resources/{resourceId}/weekly-availability/{dayOfWeek}` | Admin JWT | 요일별 반복 운영 시간 upsert. `dayOfWeek`는 Java `DayOfWeek` 값인 `1`~`7` |
| `DELETE` | `/api/resources/{resourceId}/weekly-availability/{dayOfWeek}` | Admin JWT | 요일별 운영 시간 삭제 |
| `PUT` | `/api/resources/{resourceId}/availability-exceptions/{date}` | Admin JWT | 특정 날짜 휴무/특별 운영 시간 upsert |
| `DELETE` | `/api/resources/{resourceId}/availability-exceptions/{date}` | Admin JWT | 날짜별 예외 삭제 |
| `GET` | `/api/resources/{resourceId}/reservations?date=YYYY-MM-DD` | Admin JWT | Resource별 예약 감사 목록 |

### Customer reservation

| Method | Path | Auth | 설명 |
|---|---|---|---|
| `GET` | `/api/resources/{resourceId}/slots?date=YYYY-MM-DD` | Bearer JWT | 해당 날짜의 예약 가능한 slot 목록. 관리자와 고객 모두 조회 가능 |
| `POST` | `/api/reservation-holds` | Customer JWT | 고객이 특정 slot을 임시 점유 |
| `POST` | `/api/reservation-holds/{reservationId}/confirm` | Customer JWT | 본인 hold 예약 확정 |
| `GET` | `/api/me/reservations` | Customer JWT | 내 예약 목록 |
| `POST` | `/api/me/reservations/{reservationId}/cancel` | Customer JWT | 본인 예약 취소 |

## Representative payloads

### Create tenant

```json
{
  "name": "Demo Studio",
  "slug": "demo-studio",
  "timezone": "Asia/Seoul",
  "slotDuration": 60,
  "holdTtl": 15,
  "cancellationWindow": 60,
  "admin": {
    "email": "owner@example.com",
    "password": "password123"
  }
}
```

### Create resource

```json
{
  "name": "Room A",
  "slug": "room-a",
  "description": "Consulting room"
}
```

### Weekly availability

```json
{
  "startTime": "09:00:00",
  "endTime": "18:00:00"
}
```

### Date availability exception

```json
{
  "closed": false,
  "startTime": "10:00:00",
  "endTime": "15:00:00"
}
```

휴무일은 다음처럼 표현합니다.

```json
{
  "closed": true
}
```

### Hold reservation

```json
{
  "resourceId": "00000000-0000-0000-0000-000000000000",
  "startAt": "2026-05-11T00:00:00Z"
}
```

응답의 `status`는 예약 생명주기에 따라 `HELD`, `CONFIRMED`, `CUSTOMER_CANCELLED`, `ADMIN_CANCELLED`, `CHECKED_IN`, `NO_SHOW`, `EXPIRED` 중 하나입니다.

## Error model

API 오류 응답은 Spring `ProblemDetail` 형태를 사용합니다.

| 상황 | 대표 status |
|---|---|
| request validation 실패, 잘못된 path/query 값 | `400 Bad Request` |
| 인증 실패 또는 잘못된 login body | `401 Unauthorized` |
| 역할 불일치 또는 본인 예약이 아님 | `403 Forbidden` |
| 현재 테넌트 범위에서 리소스/고객/예약 없음 | `404 Not Found` |
| tenant/resource slug 중복, 고객 email 중복, slot 충돌, 잘못된 예약 상태 전이 | `409 Conflict` |

## Review scenario

포트폴리오 리뷰어가 가장 빠르게 핵심을 확인하는 흐름입니다.

1. Swagger UI에서 전체 API surface를 확인합니다.
2. 테넌트와 관리자 계정을 생성합니다.
3. 관리자 JWT로 Resource와 Availability를 설정합니다.
4. 고객을 등록하고 고객 JWT를 발급받습니다.
5. 고객 JWT로 slots를 조회하고 reservation hold를 생성합니다.
6. 같은 slot에 대한 두 번째 hold가 `409 Conflict`로 막히는지 확인합니다.
7. hold를 confirm하고, 내 예약 목록과 관리자 감사 목록에서 상태를 확인합니다.
8. 고객 취소 후 같은 slot이 다시 조회되는지 확인합니다.

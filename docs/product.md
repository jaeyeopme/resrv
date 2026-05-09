# Product

## Problem

소규모 사업자는 전화, 메신저, 수기 장부, 스프레드시트, 업종별 SaaS를 섞어 예약을 관리하는 경우가 많습니다. 이 방식은 예약 가능 시간 계산, 중복 예약 방지, 고객 식별, 취소/변경 이력 추적이 약합니다.

`resrv`는 업종에 종속되지 않는 **resource + time slot + customer** 중심 예약 API를 제공합니다. 현재 목표는 실제 서비스의 핵심 위험인 “누가, 어떤 테넌트에서, 어떤 자원의 어떤 시간대를 점유했는가”를 서버가 일관되게 증명하는 것입니다.

## Product direction

- API-first 예약 백엔드
- 테넌트별 독립 데이터 경계
- 업종 중립적인 Resource 모델
- 로그인한 고객만 예약 가능한 신원 기반 reservation flow
- PostgreSQL 제약을 포함한 과예약 방지
- Swagger/OpenAPI로 외부 리뷰 가능한 API surface

## Users

| 사용자 | 설명 | 현재 MVP에서 가능한 일 |
|---|---|---|
| Tenant Admin | 사업자/운영자 | 테넌트 생성, Resource 관리, Availability 설정, 예약 조회 |
| Tenant Staff | 운영 직원 | `STAFF` role 기반 운영 API 접근 기반 마련 |
| Customer | 예약 고객 | 회원가입, 로그인, slot 조회, 본인 예약 hold/confirm/cancel |
| Integrator | 외부 UI/자동화 연동자 | OpenAPI를 보고 API 클라이언트나 프론트엔드 연동 가능 |

## Domain terms

| 용어 | 의미 |
|---|---|
| Tenant | 예약 서비스를 사용하는 사업자/조직 단위 |
| TenantUser | 테넌트 내부 관리자 또는 직원 계정. `OWNER`, `STAFF` role |
| Customer | 테넌트에 속한 예약 고객. 예약 API는 고객 JWT를 요구 |
| Resource | 예약 가능한 대상. 예: 좌석, 방, 장비, 담당자 |
| Weekly Availability | 요일별 반복 운영/예약 가능 시간 |
| Availability Exception | 특정 날짜의 휴무 또는 특별 운영 시간 |
| Slot | 예약 가능한 시간 구간. 테넌트 slot duration으로 계산 |
| Reservation | 고객이 점유한 예약 |
| Hold | 확정 전 임시 점유 상태. hold TTL 이후 만료 가능 |

## MVP boundary

현재 MVP는 다음 흐름을 end-to-end로 제공합니다.

1. 테넌트가 가입하고 최초 관리자 계정을 만든다.
2. 관리자가 로그인하고 Resource를 생성한다.
3. 관리자가 반복 운영 시간과 날짜별 예외를 설정한다.
4. 고객이 회원가입/로그인한다.
5. 고객이 가능한 slot을 조회한다.
6. 고객이 slot을 hold한다.
7. 동일 Resource/시간대에 대한 두 번째 활성 예약은 데이터베이스 제약으로도 막힌다.
8. 고객이 hold를 confirm한다.
9. 고객이 본인 예약을 조회/취소한다.
10. 관리자가 Resource별 예약 목록을 조회한다.

## Business rules

- 예약은 로그인한 고객만 생성할 수 있습니다. 비회원 예약 토큰 모델은 MVP에서 채택하지 않습니다.
- 고객 로그인은 check-in, 취소, 악용 방지, 감사 이력을 위한 최소 식별 장치입니다.
- 인증된 API의 테넌트 경계는 JWT `tenantId`에서만 가져옵니다.
- public API에서 필요한 테넌트 식별자는 URL의 `tenantSlug`를 서버가 해석합니다.
- Resource 생성/수정 request body는 tenant id를 받지 않습니다.
- Resource 삭제는 hard delete가 아니라 `INACTIVE` 전환입니다.
- 기본 Resource capacity는 1입니다. 다중 수용은 MVP에서 여러 Resource로 표현합니다.
- 시간 구간은 half-open interval(`[start, end)`)입니다.
- 저장은 UTC `Instant` 기준, 운영 시간 계산은 테넌트 timezone 기준입니다.
- 날짜별 Availability Exception이 있으면 weekly rule보다 우선합니다.
- 예약 hold TTL과 취소 가능 시간은 tenant 설정을 따릅니다.
- 과예약 방지는 애플리케이션 체크와 PostgreSQL exclusion constraint를 함께 사용합니다.

## What is intentionally not in Phase 1

| 제외 항목 | 이유 |
|---|---|
| 결제 | 예약 correctness와 인증 경계를 먼저 안정화하기 위함 |
| 프론트엔드 | API-first 포트폴리오 표면을 먼저 완성하기 위함 |
| 게스트 예약 | 신원 없는 예약은 취소/체크인/악용 방지에서 리스크가 큼 |
| 다중 Resource capacity | capacity 1 모델과 no-overbooking을 먼저 증명하기 위함 |
| 로그인 rate limiting / lockout | 운영 정책과 저장소 선택이 필요한 Phase 2 hardening |
| persistent JTI blacklist | DB/Redis 선택과 운영 비용 판단이 필요한 Phase 2 hardening |

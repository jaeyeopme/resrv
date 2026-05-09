# Decisions

이 문서는 오래 유지해야 하는 아키텍처 결정을 요약합니다. 자세한 근거가 필요하면 git history에서 과거 결정 문서를 확인하되, 현재 작업의 기준은 이 요약과 실제 코드입니다.

## ADR-001: shared database multi-tenancy

- 하나의 PostgreSQL 데이터베이스를 공유하되 테넌트 스코프 데이터는 `tenant_id`로 격리합니다.
- authenticated API는 JWT claim의 `tenantId`, public API는 URL slug를 서버에서 해석한 `tenantId`를 사용합니다.
- 테넌트 스코프 repository는 tenant id를 필수 입력으로 받습니다.
- unique constraint는 테넌트 스코프를 포함해야 합니다.
- request body의 tenant id는 신뢰하지 않습니다.

## ADR-002: public API tenant identification

- public login/signup API는 `/public/{tenantSlug}/...` 형태로 테넌트를 식별합니다.
- slug는 공개 식별자이며 MVP에서는 immutable로 취급합니다.
- slug를 tenant id로 변환하는 책임은 서버에 있습니다.

## ADR-003: customer account + JWT reservation flow

- 예약 생성, 확정, 조회, 취소는 로그인한 고객 JWT를 요구합니다.
- 비회원 reservation token 모델은 MVP에서 채택하지 않습니다.
- 고객 로그인은 check-in, 취소, 고객 소유권 검증, 악용 방지, 감사 이력을 위한 최소 식별 장치입니다.
- Customer는 tenant 범위 email unique를 갖습니다.
- Customer JWT는 admin JWT와 같은 token format을 쓰되 role claim이 `CUSTOMER`입니다.

## ADR-004: reservation concurrency

- 과예약 방지는 애플리케이션 중복 체크만으로 처리하지 않습니다.
- PostgreSQL partial exclusion constraint와 `tstzrange`를 사용해 활성 예약 시간 구간 충돌을 막습니다.
- 활성 충돌 대상 상태는 `HELD`, `CONFIRMED`, `CHECKED_IN`입니다.
- 예약 시간 구간은 `[start, end)`로 해석합니다.
- 만료 hold 정리는 보조 수단이며 correctness의 핵심은 데이터베이스 제약입니다.

## ADR-005: TenantUser authentication

- TenantUser는 이메일/비밀번호로 로그인합니다.
- 로그인 API는 `POST /public/{tenantSlug}/auth/login`입니다.
- 비밀번호는 Argon2id로 검증합니다.
- JWT claim은 issuer, subject, audience, issued-at, expiration, jti, tenantId, userId, role을 포함합니다.
- access token TTL은 30분입니다.
- refresh token은 MVP 범위가 아닙니다.

## ADR-006: Phase 1 JTI blacklist

- 로그아웃은 현재 Caffeine 기반 in-memory JTI blacklist로 처리합니다.
- blacklist entry는 JWT `exp`까지만 유지합니다.
- 애플리케이션 재시작 시 blacklist는 사라집니다.
- DB/Redis 기반 persistent blacklist는 별도 hardening 항목(T103)입니다.

## ADR-007: Resource identity and lifecycle

- Resource는 내부 UUID와 테넌트 범위 slug를 함께 가집니다.
- Resource slug는 사람이 다룰 수 있는 식별자로 사용할 수 있게 설계합니다.
- Resource 삭제는 hard delete가 아니라 `INACTIVE` 상태 전환으로 처리합니다.
- Resource capacity는 MVP에서 1로 고정하고, 다중 수용은 여러 Resource로 표현합니다.

## ADR-008: Availability precedence

- 특정 날짜의 Availability Exception이 weekly availability보다 우선합니다.
- `closed=true`인 exception은 해당 날짜의 slot을 만들지 않습니다.
- `closed=false`인 exception은 해당 날짜에만 적용되는 특별 운영 시간입니다.
- weekly availability는 `DayOfWeek` 기준 반복 규칙입니다.

## ADR-009: Public API documentation

- Swagger UI, OpenAPI JSON, OpenAPI YAML은 공개 접근을 허용합니다.
- 실제 business API는 Spring Security 정책과 JWT role guard를 따릅니다.
- 공개 포트폴리오 표면에서 실수로 mutating 요청을 실행하지 않도록 Swagger UI `Try it out`은 기본 비활성화합니다.

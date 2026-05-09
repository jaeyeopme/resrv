# Roadmap

## Phase 1 — Review Reservation MVP

현재 Phase 1은 “외부 리뷰어가 API와 테스트를 보고 예약 백엔드의 핵심 완성도를 판단할 수 있는 수준”을 목표로 합니다.

### Completed foundation

- 5-subproject hexagonal architecture
- PostgreSQL/Flyway 기반 schema migration
- Spring Security 기반 JWT resource server
- Tenant onboarding
- Tenant admin login/logout/me
- Resource management
- Springdoc OpenAPI/Swagger UI
- ProblemDetail 기반 오류 응답
- Testcontainers integration test 기반 검증 구조

### Completed reservation MVP

| Capability | Evidence |
|---|---|
| Customer registration/login | `CustomerWebAdapter`, `CustomerService`, `customer` table |
| Admin-only Resource management | `ResourceWebAdapter` role guard |
| Admin-only Availability write | `AvailabilityWebAdapter` role guard |
| Authenticated slot search | `GET /api/resources/{resourceId}/slots` |
| Customer reservation hold | `POST /api/reservation-holds` |
| Customer reservation confirm | `POST /api/reservation-holds/{reservationId}/confirm` |
| Customer reservation list/cancel | `/api/me/reservations/**` |
| Admin reservation audit | `GET /api/resources/{resourceId}/reservations` |
| DB-level no-overbooking | `V7__create_reservation.sql` exclusion constraint |
| End-to-end flow test | `ReservationMvpIntegrationTest` |
| OpenAPI surface test | `OpenApiIntegrationTest` |

## Product decisions already settled

- 예약은 로그인한 고객만 생성합니다.
- 비회원 예약 토큰 모델은 MVP에서 채택하지 않습니다.
- Resource capacity는 MVP에서 1입니다.
- 과예약 방지는 application check + PostgreSQL exclusion constraint를 함께 사용합니다.
- Swagger/OpenAPI는 공개 리뷰 표면으로 유지합니다.

## Phase 1 polish backlog

포트폴리오 완성도를 더 올리려면 다음이 우선입니다.

| 우선순위 | 항목 | 이유 |
|---|---|---|
| P1 | OpenAPI example payload 보강 | Swagger만 보고도 demo flow를 재현 가능하게 함 |
| P1 | README에 curl walkthrough 추가 | 리뷰어가 로컬에서 빠르게 성공 경로를 따라갈 수 있음 |
| P1 | Reservation domain/application 단위 테스트 추가 | Testcontainers 없이도 상태 전이 규칙을 빠르게 검증 가능 |
| P2 | Admin reservation status transition API | check-in/no-show/admin cancel 상태를 운영자가 다룰 수 있음 |
| P2 | Seed/demo profile | 포트폴리오 시연 시간을 줄임 |
| P2 | CI badge와 GitHub Actions 문서화 | 외부 공개 시 신뢰 신호가 됨 |

## Phase 2 — Deferred security and operations hardening

아래 항목은 기능 단계와 별도인 인증 hardening backlog입니다. 명시 요청 전에는 구현하지 않습니다.

| ID | 내용 | 이유 |
|---|---|---|
| T100 | 로그인 rate limiting | 운영 정책과 저장소/인프라 선택 필요 |
| T101 | 로그인 실패 잠금 | 사용자 상태 모델과 운영 해제 정책 필요 |
| T102 | 테넌트/관리자 활성 상태 검증 필터 | 상태 전이 정책과 API 오류 계약 확정 필요 |
| T103 | persistent JTI blacklist | DB/Redis 선택과 운영 비용 판단 필요 |

## Phase 3 — Service realism expansion

- 결제 또는 deposit 연동 전용 port 설계
- email/SMS notification outbox
- 직원별 권한 세분화
- customer profile update/password reset
- 반복 예약/패키지 예약
- 관측성: structured logging, metrics, trace id
- read-only hosted demo와 공개 Swagger URL 운영 정책

# resrv docs

이 디렉터리는 `resrv`의 외부 공유 가능한 제품/기술 문서입니다. 원본 작업 산출물을 그대로 보관하는 아카이브가 아니라, 현재 코드와 앞으로의 계획에 맞춰 큐레이션한 문서만 둡니다.

## 문서 계약

| 문서 | 역할 |
|---|---|
| [`product.md`](product.md) | 제품 의도, 사용자, 도메인 용어, MVP 경계, 핵심 비즈니스 규칙 |
| [`api.md`](api.md) | Swagger/OpenAPI 위치, 인증 모델, 현재 구현된 API surface, 리뷰 흐름 |
| [`architecture.md`](architecture.md) | 모듈 구조, 멀티테넌시, 인증, 데이터/동시성, 테스트 전략 |
| [`roadmap.md`](roadmap.md) | 현재 구현 상태와 다음 개발 단계 |
| [`decisions.md`](decisions.md) | 오래 유지해야 하는 아키텍처 결정 요약 |

## 작성 원칙

- `README.md`에는 빠른 이해와 이동 경로만 둡니다.
- `docs/`는 외부에 보여도 되는 정리된 문서만 둡니다.
- 내부 실행 상태, 드리프트, 다음 액션은 [`omx_wiki/`](../omx_wiki/README.md)에 둡니다.
- 코드가 이미 구현한 사실은 실제 코드, 테스트, Flyway migration을 우선합니다.

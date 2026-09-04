# 오공고 에이전트 작업 안내

이 문서는 Codex, Claude Code 등 팀원이 사용하는 코딩 에이전트의 공통 진입점입니다. 이 프로젝트에서는 `AGENTS.md`, `CLAUDE.md`처럼 에이전트의 작업 규칙을 제공하는 파일을 통틀어 **에이전트 지침 파일**이라고 부릅니다. 상세 규칙을 이 파일에 반복하지 않고, 주제별 기준 문서와 읽어야 하는 시점을 안내합니다.

`AGENTS.md`가 원본이며 `CLAUDE.md`는 이 파일을 가리키는 심볼릭 링크로 유지합니다.

## 작업을 시작하기 전에

1. 요청과 관련된 주제를 아래 표에서 찾습니다.
2. 연결된 기준 문서를 처음부터 끝까지 읽습니다.
3. 실제 코드와 설정을 확인한 뒤 변경합니다.
4. 문서와 요청이 충돌하면 규칙을 조용히 우회하지 말고 충돌 내용과 대안을 먼저 알립니다.
5. 변경 후 관련 테스트나 정적 검사를 실행하고, 실행하지 못했다면 이유를 남깁니다.

## 규칙과 기준 정보의 위치

| 주제 | 기준 위치 | 읽어야 하는 경우 |
| --- | --- | --- |
| 프로젝트 개요, 실행, 런타임, 모듈 구성, 스키마 운영 | [`README.md`](README.md) | 환경 구성, 실행, 배포 또는 모듈 파악이 필요할 때 |
| 레이어, 모듈 의존성, API Service와 core의 역할, Helper/Reader/Manager, Repository, 트랜잭션, OSIV, 엔티티 삭제, JPA 연관관계 | [`docs/architecture/layers-and-modules.md`](docs/architecture/layers-and-modules.md) | 기능 추가, 엔티티·연관관계·삭제 방식 변경, 구조 변경, 코드 리뷰를 할 때 |
| 예외 계층, ErrorCode, API 오류 응답, 검증 오류, Security 오류, 예외 로깅 | [`docs/architecture/error-handling.md`](docs/architecture/error-handling.md) | 예외·검증·인증/인가 실패를 추가하거나 오류 응답을 변경할 때 |
| API Request DTO, Bean Validation, Request→Command 변환 | [`docs/architecture/api-request-validation.md`](docs/architecture/api-request-validation.md) | 요청 필드·검증·Command 변환을 추가하거나 변경할 때 |
| API 성공 응답, 페이지네이션, HTTP 상태, 응답 DTO 소유 위치 | [`docs/architecture/api-response.md`](docs/architecture/api-response.md) | 도메인 API의 성공·목록 응답을 추가하거나 변경할 때 |
| REST URI, HTTP 메서드, 상태 전이 API, 멱등성 | [`docs/architecture/rest-api-design.md`](docs/architecture/rest-api-design.md) | API 경로·메서드·상태 코드를 추가하거나 변경할 때 |
| OpenAPI, Swagger UI, API 계약 인터페이스, 명세 테스트 | [`docs/architecture/openapi.md`](docs/architecture/openapi.md) | Controller·Swagger 명세·API 계약 테스트를 추가하거나 변경할 때 |
| 시간대, Clock, 현재 시각 생성, JPA Auditing, 시간 테스트 | [`docs/architecture/time-handling.md`](docs/architecture/time-handling.md) | 시간 기반 기능·엔티티 일시·스케줄러·외부 시간 연동을 추가하거나 변경할 때 |
| 사용자 인증, 렛츠커리어 로그인 연동, 토큰 수명, 내부 API 키, 사용자 역할과 기업회원 등록, 브라우저 CORS 허용 오리진 | [`docs/architecture/authentication.md`](docs/architecture/authentication.md) | 로그인·토큰·세션 흐름이나 사용자 역할을 바꾸거나, 렛츠커리어 연동 지점 또는 CORS 허용 오리진을 수정할 때 |
| 실제 Gradle 모듈과 의존성 | `settings.gradle.kts`, 루트 및 각 모듈의 `build.gradle.kts` | 모듈이나 라이브러리 의존성을 변경할 때 |

표에 없는 주제는 아직 팀 규칙으로 문서화되지 않은 상태입니다. 기존 코드만 보고 새로운 규칙을 확정하지 말고 `확인 필요`로 알립니다.

## 공통 작업 원칙

- 요청과 관계없는 대규모 수정은 하지 않습니다.
- 사용자 API와 관리자 API의 독립 배포 경계를 훼손하지 않습니다.
- `ogonggo-core`를 순수 도메인 모듈로 가정하지 않습니다. core는 Domain, 공통 Implement, JPA·MySQL Persistence를 합친 모듈이며 상세 경계는 아키텍처 문서를 따릅니다.
- 코드 배치는 현재의 재사용 횟수가 아니라 변경 이유로 판단합니다. 사용자·관리자 정책에 따라 독립적으로 변경되면 해당 API에, 행위자와 무관하게 함께 변경되면 core에 둡니다.
- API Implement는 core의 공개 계약을 사용할 수 있지만 Repository를 직접 참조하지 않습니다.
- 공개 계약, 모듈 의존성, 트랜잭션 경계를 바꾸기 전에 아키텍처 문서를 확인합니다.
- 코드 변경으로 문서의 설명이 달라지면 같은 작업에서 기준 문서도 갱신합니다.
- 미정인 내용을 임의로 확정하거나 실제로 수행하지 않은 리뷰·검증을 수행했다고 표현하지 않습니다.

## 팀 규칙을 추가하는 방법

1. 상세 내용은 적절한 `docs/` 문서에 한 번만 작성합니다.
2. 기존 기준 문서와 같은 주제라면 새 문서를 만들지 말고 기존 문서를 갱신합니다.
3. 이 파일에는 주제, 문서 위치, 읽어야 하는 경우만 표에 추가합니다.
4. 결정되지 않은 내용은 `미정`, `확인 필요`, `담당자 협의 필요`로 표시합니다.
5. 아키텍처 결정이 바뀌면 배경, 변경 이유, 영향 범위와 팀 리뷰 상태를 기준 문서에 기록합니다.

# 오공고 예외 처리 기준

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-08-28
- 적용 범위: `ogonggo-server`
- 예상 독자: 오공고 서버와 API 클라이언트를 개발·리뷰하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 모든 API 오류는 렛츠커리어와 같은 `{ status, code, message }` 계약으로 응답한다.

```json
{
  "status": 404,
  "code": "JOB_NOT_FOUND",
  "message": "일자리 공고를 찾을 수 없습니다."
}
```

- `status`는 HTTP 응답 상태와 같은 숫자다.
- `code`는 클라이언트가 분기에 사용하는 안정적인 식별자다.
- `message`는 사용자 또는 개발자가 이해할 수 있는 설명이다.
- 식별자, stack trace, 내부 예외 메시지는 응답에 포함하지 않는다.
- `errors`, `traceId`, `timestamp`, `path` 같은 추가 필드는 사용하지 않는다.

## 2. 책임과 위치

`ogonggo-core`의 `error` 패키지는 `ErrorCode` 인터페이스와 `BusinessException` 계층만 제공한다. 실제 ErrorCode enum은 오류가 발생하는 도메인이나 API 경계가 소유한다. core는 이미 Spring과 결합된 모듈이므로 `ErrorCode`가 `HttpStatus`를 직접 보유한다.

Job과 Bootcamp는 각각 `job.error.JobErrorCode`, `bootcamp.error.BootcampErrorCode`를 가진다. 인증 오류는 `auth.error.AuthErrorCode`가 소유한다. 사용자·관리자 API는 요청 형식이나 매핑처럼 도메인에 속하지 않는 전송 계층 오류만 `UserApiErrorCode`, `AdminApiErrorCode`로 관리하고, 각자의 `ErrorResponse`, `RestControllerAdvice`, Spring Security 오류 응답을 소유한다. `ErrorResponse`는 외부 계약이므로 `SuccessResponse`, `PageResponse`와 같은 각 API 모듈의 `response` 패키지에 두고 ExceptionHandler 파일에 선언하지 않는다. 두 API의 HTTP 계약은 같지만 독립 배포 경계와 Presentation 소유권을 유지하기 위해 코드를 공유 모듈로 추출하지 않는다.

```text
도메인·API별 ErrorCode → core의 BusinessException
        ↓
API별 ExceptionHandler 또는 Security Handler
        ↓
HTTP status + ErrorResponse
```

## 3. 예외 분류와 응답

| 상황 | HTTP | code |
| --- | ---: | --- |
| 검증, 바인딩, 타입, 요청 본문 오류 | 400 | `BAD_REQUEST` |
| 인증되지 않은 요청 | 401 | `UNAUTHORIZED` |
| 권한이 없는 요청 | 403 | `FORBIDDEN` |
| 도메인 리소스 없음 | 404 | 도메인별 `*_NOT_FOUND` |
| 매핑되지 않은 API 경로 | 404 | `API_NOT_FOUND` |
| 지원하지 않는 HTTP 메서드 | 405 | `METHOD_NOT_ALLOWED` |
| 중복 또는 현재 상태와 충돌 | 409 | 도메인별 코드 또는 `CONFLICT` |
| 낙관적 락 충돌 | 409 | `OPTIMISTIC_LOCK_CONFLICT` |
| 예상하지 못한 예외 | 500 | `INTERNAL_SERVER_ERROR` |

예상 가능한 실패는 상황에 맞는 도메인 ErrorCode와 `InvalidValueException`, `EntityNotFoundException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`, `InternalServerException`을 사용한다. 모든 예외 생성자는 ErrorCode를 명시적으로 받아야 하며 기본 전역 코드를 제공하지 않는다. Handler는 예외의 자체 메시지가 아니라 ErrorCode의 상태, 코드, 메시지로 응답한다.

도메인별 코드는 해당 도메인의 `error` 패키지에 둔다. 클라이언트의 대응 방식이 다르면 같은 HTTP 상태여도 별도 코드를 사용한다.

## 4. 검증 오류

Bean Validation 오류는 필드명 순으로 정렬하고 `, `로 연결한다.

```text
[page] 0 이상이어야 합니다., [size] 100 이하여야 합니다.
```

요청 형식 자체를 읽을 수 없거나 필드를 특정할 수 없으면 `BAD_REQUEST`의 기본 메시지를 사용한다. 동일한 요청은 항상 같은 순서와 형식의 메시지를 반환해야 한다.

두 파라미터의 관계처럼 단일 필드 제약으로 선언할 수 없는 Query Parameter 규칙은 각 API가 소유한 `InvalidRequestParameterException(parameterName, reason)`으로 전달한다. 이 예외도 같은 `[파라미터명] 검증 메시지` 형식의 400으로 응답하므로 클라이언트는 Bean Validation 실패와 구분하지 않아도 된다. 채용공고 달력의 `from`·`to` 관계와 기간 상한이 이 경우다.

## 5. require와 check

- `require`와 `check`는 프로그래밍 계약과 도메인 불변식의 방어 수단으로 유지할 수 있다.
- `IllegalArgumentException`과 `IllegalStateException` 전체를 400으로 변환하지 않는다.
- 클라이언트의 정상적인 요청으로 발생할 수 있고 대응이 필요한 실패는 명시적 ErrorCode와 BusinessException으로 전환한다.
- 분류되지 않은 언어 예외는 개발 오류로 보고 500으로 처리하며 내부 메시지를 숨긴다.

## 6. 로깅과 보안 오류

- 처리하는 모든 예외는 stack trace를 포함해 `error` 레벨로 기록한다.
- 인증 실패는 `AuthenticationEntryPoint`에서 401로 응답한다.
- 인증된 사용자의 권한 실패는 `AccessDeniedHandler`에서 403으로 응답한다.
- Security 필터 단계에서도 ControllerAdvice와 같은 JSON 계약과 UTF-8 인코딩을 사용한다.

## 7. 검토한 대안

| 대안 | 제외 이유 |
| --- | --- |
| ErrorResponse와 Advice를 core에 배치 | core가 HTTP Presentation까지 소유해 레이어 책임이 불분명해진다. |
| `web-common` 모듈 추가 | 현재 공유 코드의 규모에 비해 모듈·컴포넌트 스캔 복잡도가 크다. 중복 증가 시 재검토한다. |
| `GlobalErrorCode` 하나로 모든 오류 관리 | 코드의 소유 도메인과 변경 이유가 불분명해지므로 도메인·API별 enum으로 나눈다. |
| 모든 `IllegalArgumentException`을 400으로 처리 | 개발 오류를 클라이언트 오류로 숨기고 내부 메시지를 노출할 수 있다. |
| 응답에 상세 필드나 traceId 추가 | 렛츠커리어 외부 계약과 달라지므로 이번 범위에서 제외한다. |

조건이 달라져 응답 필드나 API별 오류 정책이 필요해지면 클라이언트 호환성과 두 API의 독립 변경 가능성을 함께 검토한다.

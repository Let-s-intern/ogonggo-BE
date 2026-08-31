# 오공고 API 요청과 검증

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-08-28
- 적용 범위: `ogonggo-api-user`, `ogonggo-api-admin`
- 예상 독자: API 요청과 Business/core 계약을 개발·리뷰하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> HTTP 입력 형식은 Presentation의 Request DTO에서 검증하고, 비즈니스 규칙과 도메인 상태는 Business/core에서 검증합니다.

- 모든 JSON 요청에는 `@RequestBody @Valid`를 사용합니다.
- Query Parameter와 Path Variable에 제약을 선언한 Controller에는 `@Validated`를 사용합니다.
- Request DTO는 Presentation 밖으로 전달하지 않고 값 또는 Command로 변환합니다.
- 검증 실패는 [예외 처리 기준](error-handling.md)의 400 `BAD_REQUEST` 계약을 따릅니다.

## 2. 검증 책임

| 검증 위치 | 책임 | 예시 |
| --- | --- | --- |
| Request DTO | 외부 입력의 필수 여부, 형식, 길이, 범위, 중첩 구조 | `@NotBlank`, `@Size`, `@Positive`, `@Valid` |
| Controller | Query·Path 입력과 Request→Command 변환 | 페이지 범위, 양수 식별자, `request.toCommand()` |
| Business/core | 유스케이스와 도메인 규칙, 현재 상태 | 수정 가능 상태, 기간 관계, 중복 여부 |
| `require`·`check` | 정상 요청으로 도달하면 안 되는 프로그래밍 계약 | 검증 후 필수 값 보장, 저장 후 ID 존재 |

클라이언트가 정상적으로 유발할 수 있고 대응해야 하는 비즈니스 실패는 도메인 ErrorCode와 `BusinessException`을 사용합니다. `IllegalArgumentException`이나 `IllegalStateException`을 400으로 일괄 변환하지 않습니다.

## 3. Kotlin Request DTO

```kotlin
data class CreateItemRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Positive
    val ownerId: Long,
    @field:Size(max = 20)
    @field:Valid
    val options: List<CreateOptionRequest>,
) {
    fun toCommand(): CreateItemCommand = CreateItemCommand(
        title = title,
        ownerId = ownerId,
        options = options.map(CreateOptionRequest::toCommand),
    )
}
```

- Kotlin 프로퍼티 제약에는 `@field:` 사용 지점을 명시합니다.
- 필수 문자열은 `@NotBlank`, 양수 식별자는 `@Positive`를 사용합니다.
- 문자열과 컬렉션은 도메인·DB 한계에 맞는 최대 크기를 선언합니다.
- 중첩 객체와 컬렉션 요소는 `@Valid`로 연쇄 검증합니다.
- 선택 입력은 nullable로 표현합니다.
- 생성·수정 요청에 누락 값을 임의의 기본값으로 보완하지 않습니다. Query Parameter의 공개된 기본값은 허용합니다.

요청 클래스 이름은 `CreateJobRequest`, `UpdateJobRequest`처럼 행위와 대상을 드러내고 `Dto` 접미사는 사용하지 않습니다. Request는 도메인별 `presentation/request`에 둡니다. 관련된 작은 타입은 `UserAuthRequests.kt`처럼 한 파일로 묶을 수 있지만 Controller 파일에는 선언하지 않습니다.

## 4. Request와 Command 경계

Request DTO는 HTTP 필드명, 직렬화 방식, Bean Validation 때문에 변경됩니다. Command는 Business/core가 수행하는 행위와 도메인 입력 때문에 변경됩니다. 변경 이유가 다르므로 Service가 Request DTO를 직접 받지 않습니다.

단일 값만 필요한 인증 요청처럼 별도 Command의 의미가 없으면 Controller가 검증된 값을 Service에 전달할 수 있습니다. 여러 필드를 하나의 행위 입력으로 전달할 때는 Request의 `toCommand()`에서 변환합니다.

Command는 Business 또는 core가 수행하는 행위의 입력 계약입니다. 같은 값이 여러 계층을 지난다는 이유만으로 계층마다 DTO를 만들지 않으며, Service가 단일 원시 값만 필요하면 해당 값을 직접 전달합니다.

## 5. 실패 응답

| 실패 | 응답 메시지 |
| --- | --- |
| Bean Validation | `[필드명] 검증 메시지`; 여러 건은 필드명 순으로 결합 |
| 필수 JSON 필드 누락 또는 `null` | 기본 `BAD_REQUEST` 메시지일 수 있음 |
| JSON 문법, 타입, enum·날짜 파싱 오류 | 기본 `BAD_REQUEST` 메시지 |
| Query·Path 범위 위반 | `[파라미터명] 검증 메시지` |

기본 제약 메시지를 우선 사용하고 도메인 의미가 필요한 경우에만 커스텀 메시지를 선언합니다. 클라이언트는 사람이 읽는 `message`가 아니라 `code`로 분기합니다.

## 6. 검토했지만 선택하지 않은 대안

- **렛츠커리어 구현을 그대로 복제:** 일부 `@RequestBody`에 `@Valid`가 없고 Request DTO가 Service까지 전달되어 일관된 경계를 만들기 어려워 제외했습니다.
- **모든 Request 필드를 nullable로 선언:** 누락 필드의 상세 메시지는 얻을 수 있지만 Kotlin의 타입 안전성과 변환 코드가 나빠져 제외했습니다.
- **공통 Validator 모듈 추가:** 현재 Bean Validation과 도메인 규칙으로 충분하며 공유할 별도 정책이 없어 제외했습니다.
- **입력 문자열을 전역 trim:** 사용자가 전달한 값을 암묵적으로 바꾸고 필드별 의미가 달라 제외했습니다.

새로운 입력 형식이나 반복되는 교차 필드 검증이 생기면 Request 전용 class-level constraint가 필요한지 기능별로 검토합니다.

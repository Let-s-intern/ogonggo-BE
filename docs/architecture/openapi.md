# 오공고 OpenAPI 명세

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-08-27
- 적용 범위: `ogonggo-api-user`, `ogonggo-api-admin`
- 예상 독자: API를 개발·연동·검증하는 서버·클라이언트 개발자
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 각 API 모듈은 자신의 OpenAPI 명세를 소유하고, HTTP 계약 인터페이스에 Spring MVC·검증·Swagger annotation을 함께 둡니다.

```text
UserJobApi        ← HTTP 경로, 입력, 반환형, OpenAPI 명세
      ↑
UserJobController ← @RestController, 의존성, 실제 처리
```

사용자·관리자 API는 독립 애플리케이션이므로 각각 `Ogonggo User API`, `Ogonggo Admin API` 명세와 Swagger UI를 제공합니다. core에는 OpenAPI 의존성을 넣지 않습니다.

## 2. 도구와 경로

- Spring Boot 3.2.5와 호환되는 `springdoc-openapi-starter-webmvc-ui:2.5.0`을 사용합니다.
- UI starter가 API 문서 기능을 포함하므로 `springdoc-openapi-starter-webmvc-api`를 중복 추가하지 않습니다.
- Swagger UI: `/swagger-ui.html`
- JSON 명세: `/v3/api-docs`
- YAML 명세: `/v3/api-docs.yaml`
- `/api/**`만 포함하며 `/health`는 제외합니다.
- 개발 단계에서는 기본 활성화하고 운영 환경은 `SPRINGDOC_ENABLED=false`로 비활성화할 수 있습니다.

Swagger 경로는 문서가 활성화된 환경에서 세션 없이 접근할 수 있도록 Security에서 허용합니다. API 호출 자체의 인증 정책은 바뀌지 않습니다.

## 3. API 인터페이스 규칙

- 도메인 Controller마다 `UserJobApi`, `AdminJobApi`처럼 HTTP 계약 인터페이스를 둡니다.
- 인터페이스에 `@RequestMapping`, HTTP Method Mapping, 입력 annotation, Bean Validation, `@Tag`, `@Operation`, `@ApiResponse`를 둡니다.
- 구현체에는 같은 annotation을 반복하지 않고 `@RestController`, `@Validated`, 의존성과 처리 로직만 둡니다.
- 인터페이스에는 `@RestController`나 기본 구현을 두지 않습니다.
- `@RequestParam`, `@PathVariable` 이름은 명시적으로 선언합니다.
- 반환형은 `ResponseEntity<*>`가 아니라 실제 `SuccessResponse<T>` 타입까지 명시합니다.
- 인증 Principal처럼 명세 입력이 아닌 파라미터는 `@Parameter(hidden = true)`로 숨깁니다.

Mapping과 검증 annotation을 인터페이스와 구현체에 나누거나 중복하면 Spring의 annotation 병합과 메서드 검증이 달라질 수 있으므로 한쪽에만 둡니다.

## 4. 명세 작성 범위

- Controller 인터페이스에는 기능을 구분할 수 있는 짧은 한국어 `summary`를 작성합니다.
- 사용자·관리자 명세가 분리되어 있으므로 `[사용자]`, `[어드민]` 접두사는 사용하지 않습니다.
- DTO 타입과 Bean Validation으로 알 수 있는 내용을 `@Schema`로 반복하지 않습니다.
- 의미, 단위, 예시가 타입만으로 불분명한 필드에만 `@Schema`를 사용합니다.
- 성공 응답은 구체적인 메서드 반환형에서 자동 생성합니다. 오류 `@ApiResponses` 선언으로 자동 성공 응답이 대체되는 메서드는 200 응답에 `useReturnTypeSchema = true`를 선언해 실제 반환형 스키마를 유지합니다.
- 클라이언트가 대응할 수 있는 예상 비즈니스 오류를 상태·ErrorCode와 함께 명시합니다.
- 모든 Controller에 공통 500 응답을 반복해서 나열하지 않습니다.

오류 content는 API가 소유한 `ErrorResponse` 스키마를 사용합니다. 도메인 ErrorCode를 별도 `SwaggerEnum`으로 다시 모으지 않습니다. 오류 annotation 반복이 커지면 ErrorCode를 원본으로 사용하는 자동화 방식을 검토합니다.

## 5. 인증 명세

두 API는 `BearerAuth`라는 HTTP bearer/JWT Security Scheme을 제공합니다. 인증이 필요한 API 인터페이스나 메서드에만 `@SecurityRequirement`를 선언합니다.

로그인과 토큰 재발급처럼 공개된 작업에는 Security Requirement를 붙이지 않습니다. 전역 인증 요구를 선언한 뒤 공개 작업에서 해제하는 방식은 실제 Security 정책과 어긋나기 쉬워 사용하지 않습니다.

### 토큰이 선택인 작업 — 확인 필요

채용공고 목록·상세는 로그인 없이 호출할 수 있지만, 토큰을 보내면 응답의 `bookmarked`가 채워집니다. 인증이 필수도 아니고 무의미하지도 않은 세 번째 경우입니다.

이런 작업에는 `@SecurityRequirement`를 **붙입니다.** 붙이지 않으면 Swagger UI가 토큰을 보내지 않아 `bookmarked`가 항상 `false`인 경로밖에 시험할 수 없습니다. 다만 OpenAPI에는 "선택적 인증"을 표현하는 방법이 없어, 명세만 보면 필수처럼 읽힙니다. 실제 필수 여부는 작업 description에 적습니다.

`@SecurityRequirement`를 인터페이스가 아니라 메서드에 선언하는 이유도 여기에 있습니다. 한 인터페이스 안에서 공개 작업(`GET /api/v1/jobs/calendar`), 토큰 선택 작업(`GET /api/v1/jobs`), 인증 필수 작업(`POST /api/v1/jobs/{jobId}/source-url-clicks`)이 섞입니다.

대안으로 `security: [{}, {BearerAuth: []}]`처럼 빈 요구사항을 함께 선언해 선택임을 표현하는 방법이 있지만 springdoc annotation으로는 표현이 번거롭고 도구별 해석이 갈립니다. **팀 논의 필요.**

## 6. 검증

전체 OpenAPI JSON snapshot은 작은 구현 변경에도 깨지므로 사용하지 않습니다. `/v3/api-docs` 계약 테스트에서 다음 핵심만 검증합니다.

- API별 title과 Bearer 인증 스키마
- 대표 경로와 공개·인증 작업의 Security Requirement
- 페이지 기본값과 검증 범위
- 대표 도메인 오류 응답
- `/health` 제외

Controller MVC 테스트는 실제 Mapping과 검증 동작을 별도로 보장합니다. springdoc 또는 Spring Framework 버전을 올릴 때 두 테스트를 모두 실행합니다.

## 7. 검토했지만 선택하지 않은 대안

- **Swagger annotation만 인터페이스에 배치:** Mapping과 명세가 서로 다른 파일에서 달라질 수 있어 HTTP 계약 전체를 인터페이스에 둡니다.
- **구현체에도 Mapping·검증 annotation 반복:** 중복 Mapping과 메서드 제약 재정의 위험이 있어 제외했습니다.
- **렛츠커리어의 `SwaggerEnum` 복제:** 도메인 ErrorCode와 관리 지점이 중복되어 제외했습니다.
- **사용자·관리자 통합 명세:** 실제 배포·Security 경계와 다르므로 API별로 분리합니다.
- **전체 명세 snapshot 테스트:** 변경 비용과 노이즈가 커 핵심 계약만 검증합니다.

새 API를 추가하거나 외부 계약을 변경할 때 구현과 API 인터페이스, 관련 OpenAPI 계약 테스트를 같은 작업에서 갱신합니다.

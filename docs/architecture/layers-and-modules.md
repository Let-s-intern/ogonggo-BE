# 오공고 레이어와 모듈 아키텍처

- 상태: Accepted
- 결정일: 2026-08-26
- 최종 변경일: 2026-08-28
- 적용 범위: `ogonggo-server`
- 예상 독자: 오공고 서버를 개발·리뷰하는 모든 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 사용자와 관리자의 비즈니스 흐름은 각 API 모듈이 소유하고, 두 흐름이 함께 사용하는 도메인과 구현은 core가 제공한다.

```text
ogonggo-api-user  ──┐
                    ├──> ogonggo-core
ogonggo-api-admin ──┘

Controller → API Service → API/core Implement → core Repository
```

이 결정에서 가장 중요한 규칙은 다음 네 가지입니다.

1. `ogonggo-api-user`와 `ogonggo-api-admin`은 각자의 Service를 가집니다.
2. API Service는 Repository를 직접 사용하지 않습니다.
3. core는 공통 유스케이스를 대신 수행하는 Service 모듈이 아닙니다.
4. 행위자와 무관한 공통 구현은 core에, 사용자·관리자 정책에 따라 달라지는 구현은 해당 API에 둡니다.

## 2. 왜 이렇게 나누는가

사용자와 관리자는 같은 `Job`을 사용해도 목적과 흐름이 다릅니다. 사용자는 게시된 공고를 조회하거나 북마크하고, 관리자는 초안을 만들고 수정한 뒤 게시하거나 숨깁니다. 두 흐름을 core의 하나의 Service로 합치면 한쪽의 변경이 다른 쪽에 영향을 주고 역할도 불분명해집니다.

두 API는 독립된 Spring Boot 애플리케이션과 컨테이너로 운영합니다. 사용자·관리자 트래픽과 운영 요구가 다르므로 각각 독립적으로 배포하고 scale-out할 수 있어야 합니다. 실행 단위를 분리하면 장애와 변경의 영향 범위를 줄이고, 설정·모니터링·배포를 API별로 관리하기도 쉬워집니다. 모듈 경계는 이 배포 경계와 일치시킵니다.

반면 API Service가 Repository를 직접 사용하면 유스케이스와 다음 구현 세부사항이 섞입니다.

- 조회 결과가 nullable인지 `Optional`인지
- 데이터가 없을 때 발생시킬 예외
- 게시 상태나 잠금 같은 조회 정책
- 어떤 저장 기술과 쿼리를 사용하는지

따라서 API Service에는 **누가 무엇을 어떤 순서로 하는지**를 남기고, 저장소를 사용하는 반복 구현은 core의 Implement 컴포넌트로 내립니다.

## 3. 모듈과 레이어의 책임

| 위치 | 소유 레이어 | 책임 | 대표 코드 |
| --- | --- | --- | --- |
| `ogonggo-api-user` | Presentation, Business, 사용자 Implement | 사용자 HTTP 계약·유스케이스·정책 구현 | `UserJobController`, `UserJobService` |
| `ogonggo-api-admin` | Presentation, Business, 관리자 Implement | 관리자 HTTP 계약·유스케이스·정책 구현 | `AdminJobController`, `AdminJobService` |
| `ogonggo-core` | Domain, 공통 Implement, Persistence | 공통 규칙·구현, JPA·MySQL 영속성 | `Job`, `JobReader`, `JobManager`, Repository |

### core의 성격

`ogonggo-core`는 외부 기술과 분리된 순수 도메인 모듈이 아닙니다. 공유 Domain과 공통 Implement에 JPA·MySQL 기반 Persistence를 합친 모듈입니다. 각 API 컨테이너는 core를 라이브러리로 포함하므로 런타임에는 서로 독립적으로 실행됩니다.

```text
ogonggo-core = Domain + Common Implement + Persistence(JPA/MySQL)
```

core는 엔티티 매핑과 Repository를 한 곳에서 관리해 두 API가 같은 DB를 일관되게 사용하도록 합니다. 사용자·관리자 유스케이스는 포함하지 않으며, 저장 기술을 분리할 필요가 생기면 별도 Domain/Persistence 모듈 분리를 다시 검토합니다.

### Presentation

Controller와 HTTP Request/Response DTO가 위치합니다. 입력 검증, 인증 정보 해석, 응답 변환을 담당하며 API Service만 호출합니다.

### Business

API별 유스케이스와 트랜잭션 경계를 담당합니다. Reader/Manager/Appender를 조합하지만 Repository나 다른 Business Service를 직접 호출하지 않습니다.

### Domain

모든 유스케이스에서 지켜야 하는 불변식과 상태 전이를 담당합니다. 예를 들어 모집 시작일과 종료일의 관계, 삭제된 공고의 수정 금지는 `Job`이 보장합니다.

업무 enum은 core의 `EnumField`를 구현하고 양수의 고유 `code`와 한국어 `desc`를 가집니다. 두 값은 업무 메타데이터이며 JPA 저장과 기본 JSON 응답은 기존처럼 enum 이름을 사용합니다. ErrorCode enum은 문자열 이름 기반 코드 계약이 별도로 있으므로 `EnumField`를 구현하지 않습니다.

### Implement

Business Service를 돕는 조회·생성·변경·정책 구현을 제공합니다. 사용자·관리자 정책에 따라 달라지면 해당 API에, 행위자와 무관하게 함께 변경되거나 Repository를 감싸면 core에 둡니다. 렛츠커리어의 Helper와 같은 문제를 해결하지만 책임이 드러나는 이름을 사용합니다.

### Data Access

Spring Data JPA, QueryDSL 등 저장 기술을 담당합니다. Repository는 core 내부 구현이며 기본적으로 `internal`로 둡니다.

## 4. API Service와 core는 어떻게 함께 쓰는가

두 API가 같은 core 컴포넌트를 사용하더라도 Business Service의 역할을 core에 넘기는 것은 아닙니다.

```kotlin
// ogonggo-api-user: 사용자 조회 흐름
class UserJobService(
    private val jobReader: JobReader,
    private val bookmarkReader: JobBookmarkReader,
) {
    fun getJob(userId: Long, jobId: Long): UserJobResult {
        val job = jobReader.readPublished(jobId)
        val bookmarked = bookmarkReader.isBookmarked(userId, jobId)
        return UserJobResult.from(job, bookmarked)
    }
}

// ogonggo-api-admin: 관리자 게시 흐름
class AdminJobService(
    private val jobReader: JobReader,
    private val jobManager: JobManager,
) {
    @Transactional
    fun publish(jobId: Long) {
        val job = jobReader.readForUpdate(jobId)
        jobManager.publish(job)
    }
}
```

`JobReader`는 조회 방법을 재사용하게 하고, 두 Service는 각자의 유스케이스 순서를 소유합니다. core에 `CoreJobService.getJob()`을 만들고 API Service가 그대로 위임하는 구조는 사용하지 않습니다.

## 5. Implement 컴포넌트 이름

`Helper`는 계층을 설명하는 포괄적 표현으로만 사용하고 클래스 이름에는 구체적인 책임을 표시합니다.

| 이름 | 사용할 때 | 예시 |
| --- | --- | --- |
| `Reader` | 조회, not-found 처리, 조회 정책, 잠금 | `readPublished()`, `readForUpdate()` |
| `Appender` | 신규 객체 생성과 최초 저장 | `append()` |
| `Manager` | 기존 객체 변경 또는 구현 작업 조율 | `publish()`, `hide()` |
| `Validator` | 여러 흐름이 재사용하는 검증 | `validateBookmarkable()` |
| `Policy` | 재사용 가능한 판단이나 계산 | `calculatePriority()` |

`CommonHelper`, `CoreHelper`, `UtilService`처럼 범위를 알 수 없는 이름은 사용하지 않습니다. 모든 역할을 기계적으로 분리할 필요는 없지만, 변경 이유가 둘 이상이면 책임별 분리를 검토합니다.

## 6. 의존성과 구현 규칙

- 의존 방향은 Presentation → Business → Implement → Persistence입니다.
- 두 API 모듈은 서로 참조하지 않고, core도 API 모듈을 참조하지 않습니다.
- Controller는 API/core Implement 컴포넌트나 Repository를 직접 호출하지 않습니다.
- API Service는 Repository와 다른 API Service를 직접 호출하지 않습니다.
- 사용자·관리자 정책에 따라 달라지는 구현은 해당 API Implement에 둡니다.
- 행위자와 무관하게 함께 변경되는 구현은 core Implement로 추출합니다.
- API Implement는 core의 공개 계약을 사용하며 Repository를 직접 참조하지 않습니다.
- HTTP DTO는 해당 API 모듈에 두고 core에 전달하기 전에 명령이나 값으로 변환합니다.
- 외부 HTTP 타입은 `presentation/request`, `presentation/response`에 두고, 유스케이스 `Command`·`Result`는 `business` 또는 core의 공개 계약에 둡니다.
- 계층마다 기계적으로 DTO를 만들지 않으며 Persistence projection은 core 내부 구현으로 유지합니다.
- 공통 불변식과 상태 전이는 core 도메인 모델에 둡니다.
- API에 필요한 core 계약과 도메인 타입만 공개하고 구현체와 Repository는 `internal`을 우선합니다.
- 인터페이스는 외부에 안정적인 계약을 노출할 가치가 있을 때 만듭니다. 모든 클래스에 만들지는 않습니다.

## 7. 트랜잭션과 영속성 컨텍스트 규칙

- 유스케이스 전체의 원자성을 아는 API Business Service에 `@Transactional`을 둡니다.
- 단순 조회에는 `@Transactional`과 `@Transactional(readOnly = true)`를 붙이지 않습니다.
- `readForUpdate()` 같은 잠금 조회는 호출한 Service의 트랜잭션 안에서 실행합니다.
- Implement 컴포넌트에서 근거 없이 `REQUIRES_NEW`를 사용하지 않습니다. `AFTER_COMMIT` 이벤트 리스너는 커밋된 트랜잭션에 참여할 수 없다는 근거가 있는 경우로, Spring도 이때는 `REQUIRES_NEW`나 `NOT_SUPPORTED`가 아니면 기동을 거부합니다.
- 두 API 모두 `spring.jpa.open-in-view=false`를 유지합니다.
- 비동기로 실행하는 구현은 호출자의 트랜잭션을 이어받을 수 없으므로 자신의 트랜잭션을 엽니다. 지표 기록처럼 응답의 정확성에 필요하지 않은 작업만 비동기로 두고, 응답 값과 어긋나면 안 되는 갱신은 요청 트랜잭션 안에 유지합니다.
- Controller에서 지연 로딩에 의존하지 않습니다. 응답에 필요한 연관 데이터는 Business/Implement 계층에서 fetch join, EntityGraph, projection 등 적절한 방법으로 조회합니다.
- 조회를 구현할 때 지연 로딩 예외, N+1 쿼리, DTO 변환 시점을 기능별로 검토합니다.

## 8. 엔티티 삭제와 JPA 연관관계 규칙

### 삭제

- 모든 엔티티 삭제는 소프트 삭제로 구현하며 물리적인 하드 삭제를 사용하지 않습니다.
- 삭제 시 행을 제거하지 않고 `deletedAt`과 같이 삭제 여부와 시각을 확인할 수 있는 값을 기록합니다.
- `JpaRepository.delete*`, JPQL·QueryDSL bulk delete, `deleteAllBy*` 등 실제 행을 제거하는 코드는 사용하지 않습니다.
- 일반 조회는 소프트 삭제된 데이터를 제외하며, 삭제 데이터 조회가 필요한 관리 기능은 목적이 드러나는 별도 조회로 구현합니다.
- 북마크, 태그 연결, 파트너사, 커리큘럼 같은 연결·하위 엔티티의 취소·제거·전체 교체에도 같은 원칙을 적용합니다.
- 소프트 삭제된 행과 유니크 제약이 충돌할 수 있으므로 재등록·복구 정책과 인덱스 구성을 엔티티별로 함께 검토합니다.

### JPA 연관관계

- JPA 연관관계는 기본적으로 맺지 않고 다른 엔티티의 식별자를 scalar ID 컬럼으로 보유합니다.
- 객체 탐색 편의만을 이유로 `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`를 추가하지 않습니다.
- 생명주기와 트랜잭션 경계가 실제로 일치하고, 연관관계 사용의 이점이 결합도와 조회 비용보다 명확한 경우에만 필요한 방향으로 일부 연관관계를 허용합니다.
- 연관관계를 추가할 때는 단방향·양방향 선택, fetch 전략, cascade, orphan removal, N+1과 삭제 정책의 영향을 검토하고 변경 이유를 코드 리뷰에 남깁니다.
- 연관관계가 없는 엔티티 조합과 목록 조회는 Reader 또는 전용 projection에서 명시적으로 처리합니다.

이 결정은 삭제 데이터 보존과 복구·감사 가능성을 확보하고, 엔티티 간 결합 및 예상하지 못한 JPA 로딩·cascade 동작을 줄이기 위한 것입니다. 기존 및 신규 엔티티, Repository, Implement 컴포넌트에 모두 적용하며 팀 리뷰 상태는 현재 `팀 리뷰 필요`입니다.

## 9. 코드를 어디에 둘지 판단하기

현재 여러 곳에서 사용하는지가 아니라 **같은 이유로 함께 변경되어야 하는지**를 기준으로 판단합니다.

1. 사용자 정책이 바뀔 때 함께 바뀌는가? → `api-user`의 Business 또는 Implement
2. 관리자 정책이 바뀔 때 함께 바뀌는가? → `api-admin`의 Business 또는 Implement
3. 진입점과 무관하게 항상 지켜야 하는 규칙인가? → core의 Domain
4. 행위자와 무관하게 같은 이유로 변경되는 구현인가? → core의 Implement
5. JPA·MySQL·Repository 때문에 변경되는가? → core의 Persistence 또는 Implement
6. HTTP 요청·응답이나 인증 방식 때문에 변경되는가? → 해당 API의 Presentation

두 API에서 사용한다는 사실만으로 core에 두지 않습니다. 반대로 한 API만 사용하더라도 공통 도메인 규칙이나 영속성 때문에 변경된다면 core에 둡니다. 현재는 같아 보여도 서로 다른 정책 때문에 독립적으로 바뀔 수 있다면 각 API에 유지합니다.

# 오공고 시간 처리 기준

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-08-27
- 적용 범위: `ogonggo-server`
- 예상 독자: 시간 기반 기능과 배포 환경을 개발·리뷰하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 오공고의 업무 시간은 `Asia/Seoul`을 기준으로 하며 현재 시각은 주입받은 `Clock`으로만 생성한다.

```text
기준 시간대: Asia/Seoul
업무·저장 타입: LocalDateTime, LocalDate
현재 시각 생성: LocalDateTime.now(clock), LocalDate.now(clock)
테스트 시간: Clock.fixed(..., ZoneId.of("Asia/Seoul"))
```

기존 DB와 렛츠커리어 API의 시간 표현을 유지하기 위해 `Instant` 또는 UTC 저장으로 전환하지 않는다. 타임존이 필요한 외부 시스템과 연동할 때는 경계에서 명시적으로 변환한다.

## 2. Clock 소유와 사용

`ogonggo-core`의 `time` 패키지가 Java 표준 라이브러리의 `Clock` Bean을 제공한다. 두 API는 같은 설정을 사용한다.

```kotlin
Clock.system(ZoneId.of("Asia/Seoul"))
```

- Service와 Implement는 생성자에서 `Clock`을 주입받는다.
- 현재 시각이 필요한 유스케이스는 `LocalDateTime.now(clock)`으로 한 번 생성해 같은 흐름에 전달한다.
- 도메인 엔티티에는 `Clock`을 주입하지 않는다. `close(now)`, `delete(now)`처럼 계산된 시간을 인자로 받는다.
- 운영 코드에서 인자 없는 `LocalDateTime.now()`, `LocalDate.now()`, `Instant.now()`를 사용하지 않는다.
- 호출자가 이미 기준 시각을 가진 경우 새로 생성하지 않고 전달받은 값을 사용한다.

## 3. 저장과 JPA Auditing

- 엔티티의 업무 일시와 생성·수정 일시는 `LocalDateTime`으로 저장한다.
- `createdAt`, `updatedAt`은 서비스 Clock을 사용하는 `DateTimeProvider`로 기록한다.
- DB 연결의 `serverTimezone=Asia/Seoul` 설정을 두 API에서 유지한다.
- 서버와 DB의 시간이 달라도 애플리케이션이 생성하는 현재 시각은 Clock의 서울 시간대를 따른다.

## 4. API와 외부 연동

- 공개 API의 날짜·시간은 렛츠커리어와 같이 Spring Boot/Jackson 기본 직렬화를 사용하며 전역 포맷 설정이나 커스텀 Serializer를 추가하지 않는다.
- `LocalDate`와 `LocalDateTime`은 기존 Jackson ISO-8601 형식을 유지하며 `LocalDateTime`에 offset을 임의로 추가하지 않는다.
- 클라이언트 요구나 시스템 간 호환 문제처럼 구체적인 필요가 생기면 그 영향 범위를 확인한 뒤 전역 설정 또는 경계별 포맷을 검토한다.
- 외부 API가 UTC, offset 또는 timezone 필드를 요구하면 Presentation 또는 외부 연동 어댑터에서 변환한다.
- 날짜만 의미하는 값은 `LocalDate`, 특정 시각을 의미하는 값은 `LocalDateTime`을 사용한다.

## 5. 테스트

시간 경계에 따라 결과가 달라지는 테스트는 `Clock.fixed`를 사용한다.

```kotlin
val clock = Clock.fixed(
    Instant.parse("2026-08-27T03:00:00Z"),
    ZoneId.of("Asia/Seoul"),
)
```

- 테스트 실행 시각이나 개발 장비 타임존에 의존하지 않는다.
- 시작·종료 시각과 정확히 같은 경계를 포함하는지 기능별로 검증한다.
- 단순 값 객체 테스트처럼 현재 시각이 필요하지 않으면 고정된 `LocalDateTime` 값을 직접 사용한다.

## 6. 실행 환경

- 사용자·관리자 Docker 이미지 모두 `TZ=Asia/Seoul`을 설정한다.
- JVM은 `-Duser.timezone=Asia/Seoul`로 실행한다.
- Clock이 업무 시간을 보장하지만 스케줄러, 로그와 외부 라이브러리도 같은 시간대를 사용하도록 실행 환경을 함께 고정한다.

## 7. 검토한 대안

| 대안 | 제외 이유 |
| --- | --- |
| 모든 시간을 UTC `Instant`로 저장 | 기존 DB·API와의 변환 및 마이그레이션 범위가 현재 이점보다 크다. |
| 서버 기본 시간대에만 의존 | 로컬·테스트·컨테이너 설정 차이로 결과가 달라질 수 있다. |
| 도메인 엔티티에 Clock 주입 | 도메인 객체 생성과 테스트가 Spring 구성에 결합된다. |
| 호출 위치마다 `Clock.system` 생성 | 시간 기준을 교체하거나 테스트에서 고정하기 어렵다. |

# 오공고 스케줄 작업

- 상태: Accepted
- 결정일: 2026-09-27
- 적용 범위: `ogonggo-api-user`, `ogonggo-api-admin`의 주기 작업
- 예상 독자: 스케줄 작업을 추가하거나 운영하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 스케줄 작업의 실행 주기(cron)와 켜짐 여부는 DB의 `scheduled_jobs`가 정한다. 운영자는 배포 없이 SQL로 바꾼다.

```text
API config의 ScheduledJobDefinition 빈 (이름, 기본 cron, 설명, 실행 함수)
        ↓ 기동 시 행이 없을 때만 기본값으로 생성
scheduled_jobs (name, cron, enabled)
        ↓ core ScheduledJobRegistrar가 1분마다 읽어 다시 예약
스케줄러 빈의 @SchedulerLock 메서드 (ShedLock으로 한 태스크만 실행)
```

- `@Scheduled`를 쓰지 않는다. 주기를 코드나 `application.yml`에 두면 바꿀 때마다 배포해야 한다.
- 행은 기동할 때 **없을 때만** 만든다. 이미 있는 행은 배포해도 바뀌지 않으므로 코드의 기본 cron을 바꿔도 운영 값은 그대로다.
- cron은 Spring 6자리(초 분 시 일 월 요일)이며 Asia/Seoul 기준이다.
- cron을 바꾸면 1분 안에 새 주기로 다시 예약한다. 잘못된 cron이면 기존 예약을 유지하고 `error` 로그를 남긴다. 기동 시점에 잘못되어 있으면 코드의 기본 cron으로 예약한다.
- `enabled`는 예약 시각마다 읽는다. 끄면 그 회차부터 건너뛰고 켜면 다음 회차부터 돈다.
- 행을 지우면 켜진 것으로 보고 코드의 기본 cron을 쓴다. 멈추려면 지우지 말고 `enabled`를 끈다.
- 태스크가 여럿이면 모든 태스크가 예약하고, 중복 실행은 작업 메서드의 `@SchedulerLock`이 막는다. 작업 이름은 잠금 이름과 같게 둔다.
- 작업은 전용 스레드 풀(4개)에서 돈다. 오래 걸리는 작업이 다른 작업의 예약을 막지 않게 하기 위해서다.

## 2. 작업 목록

| 이름 | API | 기본 cron | 내용 |
| --- | --- | --- | --- |
| `communityRecruitmentPostAutoClose` | 사용자 | `0 0 * * * *` | 기간이 끝난 모집글 자동 마감 |
| `jobAutoClose` | 사용자 | `0 0 * * * *` | 모집 종료 일시가 지난 모집 중 채용공고 자동 마감. 직접 마감한 것이 아니므로 `closed_at`은 남기지 않는다 |
| `bootcampAutoClose` | 사용자 | `0 0 * * * *` | 모집 종료 일시가 지난 모집 중 부트캠프 자동 마감 |
| `imageAssetCleanup` | 사용자 | `0 30 * * * *` | 쓰이지 않은 업로드 이미지 정리 |
| `work24DailyCollection` | 관리자 | `0 0 4 * * *` | 고용24 채용정보·훈련과정을 채용공고·부트캠프로 등록 |
| `letsCareerJobProfileSync` | 사용자 | `*/30 * * * * *` | 오공고에서 고친 학력·희망 조건을 렛츠커리어로 전송([인증 문서](authentication.md#전달-양쪽-아웃박스)) |

2026-09-27 이전에는 앞의 두 작업이 기동 직후부터 1시간 간격(`fixedDelay`)으로 돌았고 주기를 `application.yml`로 바꿨다. 지금은 매시 정해진 분에 돌며 해당 설정 키는 쓰지 않는다.

## 3. 작업을 추가하는 방법

1. 작업 메서드를 가진 빈을 API의 `implement`에 두고 메서드에 `@SchedulerLock(name = 작업 이름)`을 붙인다.
2. 같은 API의 `*ScheduledJobConfiguration`에 `ScheduledJobDefinition` 빈을 추가한다. `action`에는 그 빈의 메서드 참조를 넘겨야 프록시를 거쳐 잠금이 걸린다.
3. 이 문서의 작업 목록을 갱신한다.

## 4. 검토했지만 선택하지 않은 대안

- **`@Scheduled` + `application.yml`:** 운영 설정이 GitHub 시크릿으로 통째 배포되어 주기를 바꾸려면 배포가 필요하다.
- **Quartz JDBC JobStore:** 클러스터링과 이력까지 주지만 테이블 11개와 설정 비용이 현재 작업 세 개에 비해 크다.
- **관리자 API로 수정:** 운영자가 SQL로 직접 바꾸기로 해 이번에는 두지 않았다. 필요해지면 `scheduled_jobs`를 읽고 쓰는 콘솔 API를 추가한다.

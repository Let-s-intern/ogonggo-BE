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
- 태스크가 여럿이면 모든 태스크가 예약하고, 단일 실행 작업은 메서드의 `@SchedulerLock`이 중복 실행을 막는다. 작업 이름은 잠금 이름과 같게 둔다.
- 작업은 전용 스레드 풀(4개)에서 돈다. 오래 걸리는 작업이 다른 작업의 예약을 막지 않게 하기 위해서다.

## 2. 작업 목록

| 이름 | API | 기본 cron | 내용 |
| --- | --- | --- | --- |
| `recruitmentPostAutoClose` | 사용자 | `0 0 * * * *` | 기간이 끝난 모집글 자동 마감 |
| `jobAutoClose` | 사용자 | `0 0 * * * *` | 모집 종료 일시가 지난 모집 중 채용공고 자동 마감. 직접 마감한 것이 아니므로 `closed_at`은 남기지 않는다 |
| `bootcampAutoClose` | 사용자 | `0 0 * * * *` | 모집 종료 일시가 지난 모집 중 부트캠프 자동 마감 |
| `imageAssetCleanup` | 사용자 | `0 30 * * * *` | 쓰이지 않은 업로드 이미지 정리 |
| `work24DailyCollection` | 관리자 | `0 0 4 * * *` | 고용24 채용정보·훈련과정을 채용공고·부트캠프로 등록 |
| `letsCareerJobProfileSync` | 사용자 | `*/30 * * * * *` | 오공고에서 고친 학력·희망 조건을 렛츠커리어로 전송([인증 문서](authentication.md#전달-양쪽-아웃박스)) |
| `jobBookmarkAlimTalkReminder` | 사용자 | 임시 미등록 (복구 시 `0 * * * * *`) | NHN `clip_remind` 템플릿 승인 대기 중. 코드에 `ScheduledJobDefinition`을 등록하지 않아 실행되지 않음. 승인 후 등록 복구 및 DB 행 활성화 필요 |
| `jobBookmarkAlimTalkDelivery` | 사용자 | `* * * * * *` | due notification 발송. ShedLock으로 한 인스턴스만 실행하고 최대 4건 병렬 처리 |
| `notificationCleanup` | 사용자 | `0 30 3 * * *` | 최종 상태로 바뀐 지 30일 지난 알림을 500건씩 정리. ShedLock 적용 |

2026-09-27 이전에는 앞의 두 작업이 기동 직후부터 1시간 간격(`fixedDelay`)으로 돌았고 주기를 `application.yml`로 바꿨다. 지금은 매시 정해진 분에 돌며 해당 설정 키는 쓰지 않는다.

## 3. 작업을 추가하는 방법

1. 스케줄러는 HTTP의 Controller처럼 진입점이므로 도메인의 `presentation`에 두고, 작업 메서드에 `@SchedulerLock(name = 작업 이름)`을 붙인다. 스케줄러는 실행 시간·결과 기록(`SchedulerExecutionObserver`)과 로그만 맡고, 할 일은 Business Service에 맡긴다.
   - 트랜잭션은 [레이어 규칙](layers-and-modules.md#7-트랜잭션과-영속성-컨텍스트-규칙)대로 Service가 연다. core Implement는 트랜잭션을 열지 않는다.
   - 외부 호출(렛츠커리어, S3, 고용24)을 기다리는 작업은 Service 전체를 트랜잭션으로 묶지 않고, 기록이 필요한 순간에만 짧게 연다.
2. 같은 API의 `*ScheduledJobConfiguration`에 `ScheduledJobDefinition` 빈을 추가한다. `action`에는 그 빈의 메서드 참조를 넘겨야 프록시를 거쳐 잠금이 걸린다.
3. 이 문서의 작업 목록을 갱신한다.

### 알림 적재·발송 스케줄

- **임시 비활성:** NHN `clip_remind` 템플릿이 승인될 때까지 `jobBookmarkAlimTalkReminder`의 `ScheduledJobDefinition` 등록을 보류한다. 이 상태에서는 리마인드 notification을 새로 적재하지 않는다. 승인 후 코드 등록을 복구하고 `scheduled_jobs.enabled`를 확인해 활성화한다.
- 리마인드 대상 적재는 매분 스크랩 대상 500건씩 조회하고, 대상이 없거나 45초 실행 예산에 도달할 때까지 페이지를 처리한다. 별도 일정 테이블이나 영속 커서는 없다. 커서는 한 번의 실행 안에서만 이동하며, 다음 실행은 처음부터 조회해 이미 적재된 알림을 건너뛴다.
- 페이지별 알림 적재는 별도 트랜잭션으로 커밋하며 커서와 함께 저장하지 않는다. 45초 예산은 다음 페이지 진입 여부만 제한하고, 진행 중인 페이지를 중단시키는 hard timeout은 아니다. 리마인드 작업의 ShedLock 최대 보유시간은 10분이다.
- delivery dispatcher는 매초 due `PENDING` 중 `max(scheduled_at, created_at)`이 현재 시각 기준 최근 8분 이내인 행만 조회한다. 지연 적재된 알림은 적재 시각부터 처리 창을 적용하고, 두 시각 모두 8분을 넘긴 행은 `PENDING`으로 남겨 자동 발송하지 않는다. 작업 전체에 최대 90초 ShedLock을 적용해 row claim 상태 없이 한 인스턴스만 읽고 처리한다.
- provider 발송은 고정 4개 스레드의 전용 실행기로 최대 4건씩 병렬 처리하며 대기열은 두지 않는다. dispatcher는 45초 실행 예산 동안 다음 묶음을 읽지만, 이미 시작한 묶음의 `join()` 대기는 예산을 넘길 수 있어 45초는 hard timeout이 아니다. 실행기 포화면 남은 `PENDING` 행을 다음 tick에 둔다.
- 요청 결과는 provider 접수면 `SENT`, 명시적 거절이면 `FAILED`, 접수 여부를 확인할 수 없으면 `UNKNOWN`이다. 세 결과 상태는 자동 재발송하지 않는다. 단, 결과 저장 전 종료되어 `PENDING`으로 남은 건은 `max(scheduled_at, created_at)` 기준 8분 이내 조회에서 다시 처리될 수 있다. NHN 멱등성 키는 같은 요청의 중복을 최대 10분간 억제할 뿐, 영구적인 1회 발송 보장은 아니다.
- provider 접수 후 DB 결과 기록에 실패하면 행은 `PENDING`으로 남는다. `max(scheduled_at, created_at)` 기준 8분 창 안의 다음 실행에서만 재호출될 수 있고, 그 이후에는 자동 발송 대상에서 제외된다. NHN 멱등성 10분보다 2분 짧은 앱 조회 창으로 중복 가능성을 제한하며, 창이 지난 행은 삭제하지 않아 운영 조회가 가능하다.
- dispatcher 실행이 60초 이상 걸리면 종료 시 경고 로그를 남긴다. ShedLock 만료(90초) 전 지연 신호이므로 반복 발생 시 원인과 lock 시간 상향을 검토한다. 이 로그는 실행 중 실시간 watchdog 경보가 아니며, 작업이 반환된 뒤 기록된다.
- 공고 마감 변경으로 새 D-1 회차가 만들어지는 것은 이전 회차와 다른 논리 알림이다. 이미 시작된 이전 회차 발송과 새 회차 발송이 모두 이뤄지는 경우는 중복 발송으로 분류하지 않는다.
- 작업을 완전히 중지하려면 두 발송 작업 모두 `enabled=false`로 설정한다. 이미 실행 중인 dispatcher는 진행 중인 외부 요청·DB 기록이 끝나야 반환될 수 있다. 45초 예산은 강제 중단 시간이 아니다. 적재만 끄면 기존 대기열은 계속 발송된다.
- MAU 5만 기준의 실제 처리량은 DB·NHN 지연과 provider 한도에 따라 달라지며 부하 검증 전에는 보장하지 않는다.

## 4. 검토했지만 선택하지 않은 대안

- **`@Scheduled` + `application.yml`:** 운영 설정이 GitHub 시크릿으로 통째 배포되어 주기를 바꾸려면 배포가 필요하다.
- **Quartz JDBC JobStore:** 클러스터링과 이력까지 주지만 테이블 11개와 설정 비용이 현재 작업 세 개에 비해 크다.
- **관리자 API로 수정:** 운영자가 SQL로 직접 바꾸기로 해 이번에는 두지 않았다. 필요해지면 `scheduled_jobs`를 읽고 쓰는 콘솔 API를 추가한다.

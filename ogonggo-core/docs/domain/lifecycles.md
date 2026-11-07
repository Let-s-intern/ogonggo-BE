# 오공고 라이프사이클

- 최종 변경일: 2026-11-07
- 예상 독자: 상태가 있는 엔티티를 고치거나 리뷰하는 개발자
- 리뷰 상태: 팀 리뷰 필요

상태가 바뀌는 엔티티와 그 전이 규칙을 모은 문서입니다. 전이 규칙이 있는 엔티티는 다이어그램으로 그렸고, 단순히 켜고 끄는 상태나 규칙 없이 고르는 상태는 아래 표에만 적었습니다. 상태 값의 뜻은 [용어 사전](glossary.md)에 있습니다.

## 1. 다이어그램

| 대상 | 다이어그램 | 한 줄 요약 |
| --- | --- | --- |
| 채용공고·부트캠프 | [content.html](lifecycles/content.html) | 게시·검수·모집 상태가 따로 움직입니다. 기업 등록 콘텐츠만 검수를 거치고, 승인하면 바로 게시됩니다. |
| 모집글 | [recruitment-post.html](lifecycles/recruitment-post.html) | 임시저장 글은 필수 값을 채워야 게시됩니다. 숨김과 다시 공개는 운영자만 하며, 모집 상태는 게시된 글에만 있습니다. |
| 이미지 | [image-asset.html](lifecycles/image-asset.html) | 업로드 → 임시 → 첨부 순으로 바뀝니다. 첨부되지 않은 채 보존 기간이 지나면 정리 작업이 S3에서 지웁니다. |
| 렛츠커리어 전송 대기 | [letscareer-outbox.html](lifecycles/letscareer-outbox.html) | 회원마다 한 행이고, 실패하면 대기로 남아 다음 실행에서 다시 보냅니다. 닉네임 전송 대기(`LetsCareerNicknameOutbox`)도 같은 방식으로 움직입니다. |
| 렛츠커리어 콘텐츠 | [letscareer-content.html](lifecycles/letscareer-content.html) | 목록 상태와 태그 상태가 따로 움직입니다. 목록에 있고 태그가 최신이며 모집 기간인 콘텐츠만 공고별로 추천됩니다. 제목·설명이 바뀌면 다시 태그할 때까지 추천에서 빠집니다. |
| 공고 분석 | [아래 상태도](#공고-분석) | AI 서버 분석과 운영자 수정이 본문 해시를 기준으로 움직입니다. 운영자가 고친 분석은 본문이 바뀌기 전까지 AI 서버가 덮어쓰지 못합니다. |
| 알림 발송 기록 | [아래 상태도](#알림-발송-기록) | 발송 전에 `PROCESSING`으로 선점하며, stale 복구와 늦은 provider 결과를 attempt 단위로 구분합니다. |
| 광고 계약 | [아래 상태도](#광고-계약) | 신청 폼 계약은 운영자가 상태를 바꾸고, 비즈니스 공고 계약은 공고 검수 상태를 그대로 따릅니다. |

분리된 Archify 다이어그램의 `SRC` 배지를 누르면 근거가 된 코드 줄로 이동합니다. GitHub에서는 HTML이 렌더링되지 않으니 내려받아 브라우저로 엽니다. 알림 상태도의 근거는 [`Notification`](../../src/main/kotlin/com/ogonggo/core/notification/domain/Notification.kt)과 [`NotificationManager`](../../src/main/kotlin/com/ogonggo/core/notification/delivery/implement/NotificationManager.kt)입니다.

### 공고 분석

```mermaid
stateDiagram-v2
    [*] --> 분석없음: 공고 등록
    분석없음 --> AI분석: AI 서버 저장
    분석없음 --> 운영자수정: 관리자 분석 수정
    AI분석 --> AI분석: AI 서버 저장(같은 본문, 다시 분석)
    AI분석 --> 운영자수정: 관리자 분석 수정
    운영자수정 --> 운영자수정: 관리자 분석 수정 / AI 서버 저장은 409
    AI분석 --> 낡은분석: 본문 변경
    운영자수정 --> 낡은분석: 본문 변경(관리자 본문 수정·AI 서버 재수집)
    낡은분석 --> AI분석: AI 서버 다시 분석(운영자 수정 표시 지움)
    낡은분석 --> 운영자수정: 관리자 분석 수정
```

- 상태는 칼럼 하나가 아니라 `job_analyses` 행의 유무, `content_hash`와 지금 본문 해시([`Job.contentHash`](../../src/main/kotlin/com/ogonggo/core/job/domain/Job.kt))의 일치, `edited_at` 유무로 정해집니다. 근거는 [`JobAnalysis`](../../src/main/kotlin/com/ogonggo/core/job/domain/JobAnalysis.kt)와 [`JobAnalysisManager`](../../src/main/kotlin/com/ogonggo/core/job/implement/JobAnalysisManager.kt)입니다.
- 낡은 분석은 사용자 상세에 보이지 않고 AI 서버 분석 대상이 됩니다. 관리자 상세는 낡은 분석도 싣고 `analysisCurrent=false`로 알립니다.
- 운영자 수정은 늘 지금 본문 해시로 저장하므로 곧바로 사용자에게 보입니다. 운영자 수정도 본문이 바뀌면 낡습니다. 운영자가 관리자 콘솔에서 제목·본문을 고친 경우도 같아서, 고친 본문에 맞는 분석을 바로 보이려면 분석도 함께 고칩니다(LC-3549, 2026-11-05까지는 운영자가 고친 분석을 새 본문 해시로 옮겼습니다). 본문 해시가 그대로인 동안 AI 서버 분석 대상에서 빠지고 AI 서버 저장은 409 `JOB_ANALYSIS_EDITED_BY_ADMIN`입니다(LC-3567).
- 운영자 분석 수정과 AI 서버 저장은 공고 행을 잠가(`JobReader.readForUpdate`) 차례로 돕니다. 분석 행은 첫 저장 전에는 없어 잠글 수 없으므로 공고 행으로 묶습니다. 그래서 겹쳐 들어와도 AI 저장이 막 고친 운영자 수정을 지우지 않고, 첫 저장 둘이 `job_id` 유일 제약에 걸려 500이 나지 않습니다. 검증은 `concurrency` 태그의 `JobAnalysisConcurrencyTest`입니다.

### 광고 계약

```mermaid
stateDiagram-v2
    [*] --> 신청: 광고 신청 폼 제출 / 비즈니스 공고 등록
    신청 --> 협의중: 운영자 (신청 폼만)
    협의중 --> 승인: 운영자
    협의중 --> 반려: 운영자
    신청 --> 승인: 운영자 / 공고 검수 승인
    신청 --> 반려: 운영자 / 공고 검수 반려
    승인 --> 신청: 공고 수정·검수 되돌리기
    반려 --> 신청: 반려된 공고 수정
```

- 신청 폼 계약(`APPLICATION_FORM`)은 운영자가 바꾸며, 잘못 바꾼 상태를 되돌릴 수 있도록 어느 상태로든 바꿀 수 있습니다(`AdvertisementContract.changeStatus`).
- 비즈니스 공고 계약(`COMPANY_POSTING`)은 공고 검수 상태를 그대로 따릅니다(`AdvertisementContract.followReview`). 운영자가 계약 상태를 직접 바꾸면 409 `ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW`이고, 검수 화면에서 승인·반려합니다. 검수 상태를 바꾸는 `JobManager`·`BootcampManager`의 승인·반려·검수 요청이 같은 트랜잭션에서 맞춥니다. 승인된 공고를 고쳐 다시 검수를 받는 동안에도 신청으로 돌아가는데, 그동안 공고가 노출되지 않기 때문입니다.
- 계약이 없는 비즈니스 공고(이관 SQL 적용 전 등록분)는 검수 상태만 바뀌고 계약은 건너뜁니다. 이관 SQL이 나중에 현재 검수 상태로 계약을 만듭니다.

### 알림 발송 기록

```mermaid
stateDiagram-v2
    [*] --> PENDING: 알림 적재
    PENDING --> PROCESSING: due 행 원자 선점
    PROCESSING --> SENT: provider 접수 확인
    PROCESSING --> FAILED: 명시적 발송 거절
    PROCESSING --> UNKNOWN: 결과 미확정 또는 안전한 stale 복구 불가
    PROCESSING --> PENDING: NHN 멱등성 창 안에서 재시도
    UNKNOWN --> SENT: 같은 시도의 늦은 접수 결과
    UNKNOWN --> FAILED: 같은 시도의 늦은 명시 거절
    UNKNOWN --> UNKNOWN: 같은 시도의 늦은 미확정 결과
```

`PROCESSING`은 provider 요청을 시작하기 전에 기록하고, provider별 stale timeout은 이 시작 시각부터 계산합니다. timeout은 실패 증거가 아닙니다. NHN만 최초 시도 시각 기준 멱등성 창 안에서 제한 재시도하며, 재시도하지 않은 `UNKNOWN`은 같은 시도의 늦은 결과가 오면 정정할 수 있습니다. `UNKNOWN` 결과나 업무 발송 기한 만료는 자동 재발송하지 않습니다.

## 2. 다이어그램이 없는 상태

| 대상 | 상태 | 그리지 않은 이유 |
| --- | --- | --- |
| 회원 `User` | 활성 / 탈퇴 / 정지 | 탈퇴한 회원은 정지·활성화할 수 없다는 규칙은 있지만, 지금은 이 전이를 호출하는 API가 없습니다. 확인 필요: 회원 탈퇴·정지 기능의 도입 계획 |
| 공고·부트캠프 북마크 | 지원 단계 / 신청 단계 | 회원이 아무 단계로나 옮길 수 있어 전이 규칙이 없습니다. 북마크를 다시 하면 스크랩으로 돌아갑니다. |
| 모집글 지원 기록 | 진행 단계 | 북마크와 같이 규칙 없이 옮깁니다. 삭제된 기록만 바꿀 수 없습니다. |
| 공지, 취준고민 | 노출 / 숨김 | 운영자가 켜고 끄는 값 하나뿐입니다. |
| 채용공고 검수 완료 `Job.inspectedAt` | 미검수 / 검수 완료 | 운영자가 켜고 끄는 값 하나뿐입니다. 이미 완료한 공고를 다시 완료해도 처음 시각과 관리자를 지킵니다. 노출·검수 상태와 따로 움직이며, 운영자 내용 수정은 이 값을 지우지 않습니다. AI 서버 재수집이 본문(제목과 본문 여덟 칸)이나 원문을 바꾸면 지웁니다. 검수 완료·해제는 공고 수정 일시(`updated_at`)를 바꾸지 않아 공고 분석 후보로 다시 읽히지 않습니다(LC-3567). |
| 채용공고 운영자 내용 수정 `Job.contentEditedAt` | 수정 없음 / 운영자 수정 | 관리자 콘솔의 부분 수정이 제목·본문·공고 정보·원문을 하나라도 고치면 켜지고 마지막 시각과 관리자로 바뀝니다. 끄는 경로가 없습니다. 켜져 있는 동안 AI 서버 교체·원문 변경은 공고 내용과 원문을 바꾸지 않습니다(`AiJobService.replace`, LC-3567). |
| 광고 문의 `AdvertisementInquiry` | 접수 / 연락함 / 집행 완료 / 반려 | 새 문의는 접수로 시작하고, 영업이 잘못 바꾼 상태를 되돌릴 수 있도록 어느 상태로든 옮깁니다. 전이 규칙이 없습니다. 메뉴의 오늘 문의 수는 반려한 문의를 빼고 세므로 반려로 바꾸면 줄고 반려에서 되돌리면 다시 늡니다. |

### 전체 토픽 구독 동기화

```mermaid
stateDiagram-v2
    [*] --> Pending: 토큰 등록·변경 / 최초 구독 / 회원 상태 변경
    Pending --> Synced: 같은 revision의 FCM 성공 / 원문 제거
    Pending --> Pending: 실패 / 횟수·다음 시각 기록
    Synced --> Pending: 구독 희망 상태 변경 / revision 증가
```

- `FcmTopicSyncTask.pending=true/false`를 Pending/Synced로 표시한다. subscribe/unsubscribe는 `desiredSubscribed`로 구분하고 결과는 같은 revision에만 반영한다.
- 구독 실패 재처리와 `Notification`의 푸시 결과 상태는 독립적이다. 구독 실패는 5초부터 최대 1시간 간격으로 재처리한다.

## 3. 다이어그램 고치기

다이어그램은 [Archify](https://github.com/tt-a1i/archify)로 만듭니다. 원본은 같은 폴더의 `*.archify.json`이고, HTML은 여기서 만든 결과물입니다.

1. 상태 전이 코드를 바꾸면 해당 JSON의 상태·전이와 `sources`의 줄 번호를 고칩니다.
2. `meta.repository.revision`을 근거 코드가 들어 있는 커밋으로 바꿉니다. Archify는 이 커밋에서 줄 번호를 검증합니다.
3. 저장소 루트에서 다음 명령을 실행합니다.

```bash
node <archify 설치 경로>/bin/archify.mjs finalize lifecycle ogonggo-core/docs/domain/lifecycles/<이름>.archify.json ogonggo-core/docs/domain/lifecycles/<이름>.html --repo-root . --quality showcase
```

검증 기록 파일(`*.finalize.json` 등)은 `.gitignore`에 있어 커밋되지 않습니다.

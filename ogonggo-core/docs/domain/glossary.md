# 오공고 용어 사전

- 최종 변경일: 2026-10-27
- 예상 독자: 오공고를 처음 맡는 개발자, 기획·디자인과 대화하는 개발자
- 리뷰 상태: 팀 리뷰 필요

기획에서 쓰는 말과 코드 이름을 짝지은 사전입니다. 코드를 읽다가 모르는 이름이 나오거나, 기획 용어를 코드에서 찾을 때 씁니다. 도메인 사이의 관계는 [도메인 개념도](domain-map.md)를 봅니다.

새 개념을 추가하거나 이름을 바꾸면 이 표도 함께 고칩니다. enum 값 전체는 코드가 기준이며, 여기에는 이해에 필요한 값만 적습니다.

## 1. 채용 콘텐츠

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 채용공고, 공고 | `Job` | 회사의 채용 정보. 회원은 원문 링크로 나가서 지원합니다. |
| 직군 / 직무 | `JobField` / `JobRole` | 직군은 큰 분류(IT·개발, 마케팅·광고 등), 직무는 직군 아래의 세부 분류(백엔드 등)입니다. 직무마다 속한 직군이 정해져 있습니다. 2026-10-12 AI·데이터·디자인·상품기획·MD·회계·세무·재무·고객상담·TM·미디어·엔터 직군을 IT·개발·기획·전략·HR·총무·마케팅·광고로 합치고 그 직무를 옮겼습니다. 2026-10-15 IT·개발 직무를 16개로 묶고 기획이 뺀 직무를 지웠습니다. |
| 고용 형태 | `JobEmploymentType` | 정규직, 계약직, 인턴, 파트타임, 일학습병행, 미래내일 일경험, 기타 |
| 경력 유형 | `JobExperienceType` | 신입, 경력, 신입·경력, 경력 무관 |
| 모집 기간 유형 | `JobRecruitmentType`, `BootcampRecruitmentType` | 기간 모집(마감일 있음) 또는 상시 모집 |
| 공고 분석 | `JobAnalysis` | 채용공고를 AI로 요약한 결과. AI 서버가 만들어 보내며, 공고마다 하나입니다. 본문 해시가 달라지면 보여 주지 않고 다시 요약합니다. |
| 태그 | `Tag`, `JobTag` | 공고에 붙는 자유 태그 |
| 공고 본문 항목 | `JobContentField` | 회사 및 팀 소개, 주요 업무, 자격 요건, 우대 사항 등 공고 상세의 구획 |
| 부트캠프 | `Bootcamp` | 취업 준비생을 위한 교육 과정. 공고처럼 외부 페이지에서 신청합니다. |
| 부트캠프 분류 | `BootcampCategory` | KDT(고용24에서 수집한 K-디지털 트레이닝), 새싹(크롤러가 수집한 과정). 저장값이 아니라 조회할 때 등록 경로로 가릅니다. |
| 커리큘럼 / 파트너사 | `BootcampCurriculum` / `BootcampPartner` | 부트캠프의 회차별 교육 내용 / 참여 기업 |
| 운영 방식 | `BootcampOperationType` | 온라인, 오프라인, 온·오프라인 |
| 수강료 유형 | `BootcampTuitionType` | 무료, 유료, 국비 지원 |
| 지표 | `JobMetric`, `BootcampMetric` | 조회 수·북마크 수 같은 집계. 인기순 정렬에 씁니다. |

## 2. 상태

같은 콘텐츠에 상태가 여러 개 있어 헷갈리기 쉽습니다. **모집 상태는 시간이, 게시 상태는 사람이, 검수 상태는 관리자가** 바꾼다고 기억하면 됩니다. 상태가 바뀌는 흐름은 [라이프사이클](lifecycles.md)에 있습니다.

| 용어 | 코드 | 값 | 설명 |
| --- | --- | --- | --- |
| 모집 상태 | `JobRecruitmentStatus`, `BootcampRecruitmentStatus`, `RecruitmentPostRecruitmentStatus` | 모집 중, 마감 | 마감일이 지나면 스케줄 작업이 마감으로 바꿉니다. |
| 게시 상태 | `JobPublicationStatus`, `BootcampPublicationStatus`, `RecruitmentPostPublicationStatus` | 초안(임시저장), 게시(공개), 숨김(비공개) | 회원에게 보이는지 여부입니다. |
| 검수 상태 | `ContentReviewStatus` | 검수 대기, 승인, 반려 | 기업 회원이 올린 콘텐츠에만 있고 나머지는 `null`입니다. 승인해야 게시할 수 있습니다. |
| 반려 사유 | `ContentRejection` | | 관리자가 반려하며 남긴 말. 콘텐츠마다 한 행입니다. |
| 등록 경로 | `ContentSource` | 크롤링(`CRAWLER`), 고용24(`WORK24`), 비즈니스 등록(`COMPANY`) | 콘텐츠가 들어온 곳. 등록 후 바뀌지 않습니다. |
| 회원 상태 | `UserStatus` | 활성, 탈퇴, 정지 | |

## 3. 모집글과 취준고민

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 모집글 | `RecruitmentPost` | 사이드 프로젝트·스터디 팀원을 모으는 글 |
| 모집 유형 | `RecruitmentPostType` | 사이드 프로젝트, 스터디 |
| 모집 포지션 | `RecruitmentPostPosition` | 백엔드, 프론트엔드, 디자인, 기획, 마케팅, 기타 |
| 연락 수단 | `RecruitmentPostContactMethod` | 카카오톡 오픈채팅, 이메일, 슬랙, 디스코드 |
| 모집글 지원 기록 | `RecruitmentPostApplication` | 회원이 모집글의 연락처를 열었거나 스크랩을 지원 준비로 옮긴 기록. **실제 지원서 제출이 아닙니다.** |
| 댓글 신고 | `RecruitmentPostCommentReport` | 모집글 댓글에 대한 신고 |
| 취준고민, 고민글 | `Concern` | 취업 준비 고민을 묻는 게시판 글. 본문은 모집글처럼 Lexical EditorState JSON이고, 사진은 본문 밖 첨부입니다. |
| 고민글 사진 | `ConcernImage` | 고민글에 첨부한 사진. 본문 밖에 최대 3장, 순서대로 보입니다. |
| 모집글 사진 | `RecruitmentPostImage` | 모집글에 첨부한 사진. 본문 밖에 최대 3장, 순서대로 보입니다. |
| 답변 / 답글 | `ConcernComment` | 고민글의 부모 댓글이 답변, 답변에 단 1단계 대댓글이 답글입니다. |
| 공식 답변 | `ConcernComment.official` | 작성 당시 관리자가 쓴 답변. 나중에 역할이 바뀌어도 그대로 둡니다. |
| 고민 카테고리 | `ConcernCategory` | 공고 질문, 직무·커리어, 서류·면접, 사이드·경험, 기타 |

## 4. 사용자 활동

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 북마크, 스크랩 | `JobBookmark`, `BootcampBookmark`, `RecruitmentPostBookmark` | 회원이 저장한 콘텐츠. 공고·부트캠프 북마크는 지원 단계도 함께 가집니다. |
| 지원 단계 (공고) | `JobApplicationStatus` | 스크랩 → 지원 준비 중 → 지원 완료 → 면접 → 합격 / 불합격. 회원이 직접 옮깁니다. |
| 신청 단계 (부트캠프) | `BootcampApplicationStatus` | 스크랩 → 신청 전 → 신청 완료 → 활동 중 → 활동 완료 |
| 진행 단계 (모집글) | `RecruitmentPostApplicationProgressStatus` | 지원 준비 중 → 지원 완료 → 활동 중 → 활동 완료 |
| 원문 이동 | `SourceUrlClick` | 회원이 공고 원문이나 부트캠프 지원 페이지로 나간 기록. 취소할 수 없고 회원·콘텐츠마다 한 행입니다. |
| 채용공고 달력 | `JobCalendarSearchCondition` | 공고를 모집 기간·마감일 기준으로 달력에 펼쳐 보는 화면. 북마크한 공고만 거를 수 있습니다. |

## 5. 회원과 외부 연동

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 회원 | `User` | 오공고 계정. 일반 회원, 멘토, 기업 회원, 관리자를 역할로 구분합니다. |
| 일반 회원 | `UserRole.USER` | 렛츠커리어 계정과 1:1로 대응하는 회원. 오공고는 비밀번호를 갖지 않습니다. |
| 기업 회원, 비즈니스 회원 | `UserRole.COMPANY` | 공고·부트캠프를 직접 등록하는 회원. 오공고가 이메일·비밀번호를 직접 가집니다. |
| 멘토 | `UserRole.MENTOR` | 렛츠커리어 멘토. 일반 회원과 같은 기능을 쓰고 댓글에 멘토로 표시됩니다. 최초 가입 때 렛츠커리어 멘토면 부여하고, 이후에는 관리자가 바꿉니다. |
| 관리자 | `UserRole.ADMIN` | 관리자 콘솔을 쓰는 운영자. 댓글에는 멘토보다 위 등급으로 표시됩니다. |
| 회원 프로필 | `UserProfile` | 렛츠커리어 프로필의 사본과 학년·희망 조건 같은 오공고 정보 |
| 기업 프로필 | `CompanyProfile` | 기관명, 로고, 담당자 정보와 가입할 때 받는 사업자 번호, 기업 형태, 직원 수, 산업, 주소, 홈페이지, 담당자 직책·담당 업무, 추천인. 가입 정보는 형식만 검사하고 진위는 확인하지 않으며, 칼럼을 더하기 전에 가입한 회원은 비어 있습니다. |
| 사업자 번호 종류 | `CompanyBusinessNumberType` | 사업자등록번호(`BUSINESS_REGISTRATION`, 숫자 10자리), 법인등록번호(`CORPORATE_REGISTRATION`, 숫자 13자리), 해외 기업(`OVERSEAS`, 번호 없음). 번호는 종류 없이 둘 수 없고 숫자만 저장합니다. 한 회사에 담당자 계정이 여럿일 수 있어 번호는 유니크가 아닙니다. |
| 기업 형태 | `CompanyType` | 대기업, 중견기업, 중소기업, 스타트업, 외국계, 공공기관·공기업, 비영리·단체. 회원이 고른 값입니다. |
| 직원 수 구간 | `CompanyEmployeeCountRange` | 1~9명, 10~49명, 50~99명, 100~299명, 300~999명, 1,000명 이상 |
| 담당 업무 | `CompanyManagerDuty` | 기업 회원 담당자의 업무. 인사·채용, 대표·경영, 마케팅·홍보, 기타. 직책(`managerTitle`, 자유 입력)과 따로 고릅니다. |
| FCM 토큰 | `UserFcmToken` | 회원의 설치별 푸시 수신 토큰. 해제하면 원문·활성 해시·활성 슬롯을 비우고 행과 해제 시각을 남깁니다. |
| 설치 식별자 | `UserFcmToken.deviceId` | 클라이언트가 설치별로 생성·유지하는 UUID. 같은 설치의 토큰 갱신과 계정 전환을 식별합니다. 기존 클라이언트는 null입니다. |
| 활성 토큰 슬롯 | `UserFcmToken.activeSlot` | 설치 UUID 또는 `legacy:{userId}`인 DB 중복 방지 키. 활성 토큰 해시와 각각 유니크 제약을 가집니다. |
| 학년 | `UserGrade` | 1~4학년, 5학년 이상, 졸업생 |
| 렛츠커리어 | | 오공고와 연결된 외부 서비스. 일반 회원의 로그인과 프로필을 소유합니다. |
| 렛츠커리어 전송 대기 | `LetsCareerJobProfileOutbox` | 렛츠커리어로 보낼 학력·희망 조건 변경. 회원마다 한 행이고 스케줄 작업이 최신 값을 보냅니다. |
| 닉네임 전송 대기 | `LetsCareerNicknameOutbox` | 렛츠커리어로 보낼 닉네임 변경. 회원마다 한 행이고 스케줄 작업이 최신 닉네임과 수정 일시를 보냅니다. |
| 가입 경로 | `LetsCareerAuthProvider` | 렛츠커리어 가입 경로(카카오, 네이버, 구글, 이메일) |
| 유입 구분 | `UserInflowSource` | 렛츠커리어 기존 회원(`LETSCAREER`)인지 오공고로 처음 들어온 회원(`OGONGGO`)인지. 렛츠커리어 계정이 처음 로그인한 서비스로 정하고, 그 기록이 없는 과거 계정만 가입 일시 차이(1시간)로 정합니다. |
| 고용24 | `ContentSource.WORK24` | 정부 고용 정보 포털. 채용공고와 훈련 과정을 수집합니다. |
| 외부 식별값 | `externalId` | 고용24가 매긴 공고·과정 번호. 같은 콘텐츠를 다시 수집하지 않는 데 씁니다. |

## 6. 추천

오공고가 회원에게 "이걸 보세요"라고 골라 주는 개념입니다. 고르는 주체와 저장 여부가 서로 달라 헷갈리기 쉽습니다.

| 용어 | 코드 | 누가 고르나 | 설명 |
| --- | --- | --- | --- |
| 오늘의 공고 | `TodayJob` | 운영자 | 운영자가 고른 오늘의 추천 공고 목록. 공고마다 카드에 보여 줄 추천 문구(제목 30자·설명 200자)를 함께 둡니다. 목록을 바꿀 때마다 기존 행을 모두 지우고 새로 넣으며, 회원에게는 그중 게시 중인 공고만 보입니다. |
| 렛츠커리어 콘텐츠 | `LetsCareerContent` | | 렛츠커리어의 프로그램·자료집·블로그의 사본. 스케줄 작업이 렛츠커리어 목록으로 덮어쓰고, 목록에서 빠진 콘텐츠는 지운 것으로 표시했다가 다시 나오면 되살립니다. |
| 콘텐츠 종류 | `LetsCareerContentKind` | | 챌린지, 라이브, VOD, 가이드북, 서류 진단, 무료 자료집, 블로그. 추천할 때는 프로그램(앞의 다섯)·자료집·블로그 세 갈래로 묶어 섞습니다. |
| 추천 태그 | `LetsCareerContentTags` | AI 서버 | 콘텐츠에 붙인 직군·직무·준비 단계. 직군·직무는 채용공고와 같은 enum이고, 비어 있으면 모든 직무용입니다. 콘텐츠 제목·설명이 바뀌면 다시 태그합니다. 태그가 없는 콘텐츠는 추천하지 않습니다. |
| 준비 단계 | `LetsCareerContentTopic` | | 콘텐츠가 다루는 취업 준비 단계(이력서, 자기소개서, 포트폴리오, 면접, 인적성·필기 등). 공고 분석의 제출 서류·전형과 맞춰 봅니다. |
| 공고별 렛츠커리어 콘텐츠 추천 | `LetsCareerContentRecommendPolicy` | 오공고 | 채용공고 상세에서 그 공고에 맞는 콘텐츠 3개. 모집 기간인 콘텐츠 중 직무 일치 +5, 직군 일치 +3, 준비 단계 일치 하나에 +2(최대 두 개)로 점수를 매기고, 한 갈래가 두 개를 넘지 않게 섞습니다. 저장하지 않고 공고를 열 때마다 계산하며, 같은 공고는 늘 같은 결과가 나옵니다. |
| 추천 챌린지 | `challenge` 패키지 | 렛츠커리어 (무작위) | 렛츠커리어에서 모집 중인 챌린지 최대 3개. 지금은 렛츠커리어 서버가 회원과 상관없이 무작위로 고릅니다. 로그인한 일반 회원이면 렛츠커리어 회원 식별자를 함께 넘기지만 아직 고르는 데 쓰지 않습니다. 오공고는 저장하지 않고 화면을 열 때마다 받아 와 그대로 보여 주며, 실패하면 빈 목록으로 응답합니다. |

## 7. 알림

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 사용자 알림함 | `UserNotification` | 수신자에게 보관하는 댓글 소식 또는 관리자 메시지 한 건. 읽음은 `readAt`으로 표시하며, 토큰·푸시 결과와 독립적으로 최근 30일을 조회합니다. |
| 알림 종류 | `UserNotificationType` | 모집글 부모 댓글의 답글(`SIDE_STUDY_COMMENT_REPLY`), 취준고민 최상위 댓글(`CONCERN_COMMENT`), 관리자 메시지(`ADMIN_NOTIFICATION`) |
| 푸시 생략 사유 | `UserNotificationPushSkipReason` | 생성 시 활성 토큰이 없는 `NO_ACTIVE_TOKEN`. 이후 토큰 등록 때 재발송하지 않습니다. |
| 발송 묶음 | `NotificationBatch` | 어드민의 FCM 요청 운영 기록. 회원별 알림함과 토큰별 발송은 각각 `UserNotification.batchId`·`Notification.batchId`로 묶으며 DIRECT 적재 수는 토큰 수, TOPIC 적재 수는 1건입니다. 토픽 배치는 공통 공지 원문으로 사용하고 회원별 읽음은 NotificationBroadcastRead에 저장합니다. |
| 공통 공지 읽음 | `NotificationBroadcastRead` | 전체 토픽 공지를 읽은 회원과 배치·최초 읽음 시각. 읽기 전에는 회원별 행을 만들지 않습니다. |
| 토픽 구독 동기화 | `FcmTopicSyncTask` | 토큰별 최신 구독 희망 상태와 요청 revision, 실패 재처리 정보. FCM 접수 결과와 사용자 읽음 상태와는 별개입니다. |
| 발송 기록 | `Notification` | 개별 주소 또는 FCM 토픽의 발송 대기·처리 중·결과. `PROCESSING`은 provider 요청 후 결과를 기다리는 상태이며, 알림함이 있으면 `userNotificationId`로 연결합니다. |
| 알림 처리 중 | `NotificationStatus.PROCESSING` | provider 요청을 시작해 응답 또는 결과 저장을 기다리는 상태. provider별 stale timeout을 넘기면 해당 provider 정책으로 복구합니다. |
| 알림 provider | `NotificationProvider` | 알림 발송을 맡는 시스템. 현재 NHN 알림톡, SMTP 이메일, Firebase FCM이며 stale 처리 정책을 각각 설정합니다. |
| 제공자 명시적 발송 거절 | `NotificationFailureCategory.PROVIDER_REJECTED` | NHN `-1031`처럼 전체 수신자의 실패가 확인된 결과. 세부 실패 원인을 모르는 경우에도 접수 여부 미확정인 `UNKNOWN`과 구분해 `FAILED`로 기록합니다. |

## 8. 운영·기반

| 용어 | 코드 | 설명 |
| --- | --- | --- |
| 공지 | `Announcement` | 관리자가 올리는 공지. 노출 여부와 상단 고정을 따로 관리합니다. |
| 서비스 의견 | `ServiceFeedback` | 좋은 점·아쉬운 점 의견. 비로그인으로도 남길 수 있습니다. |
| 광고 문의 | `AdvertisementInquiry` | 기업 담당자가 남긴 광고 문의. 무료 홍보 신청을 포함한 광고 문의 전체를 한 테이블에 두고 문의 유형과 캠페인 코드로 나눕니다. 저장한 뒤 Slack과 확인 메일로 알립니다. 비회원도 남길 수 있습니다. |
| 문의 유형 | `AdvertisementInquiryType` | 무료 홍보(`FREE_PROMOTION`), 출시 알림(`LAUNCH_ALERT`). 유료 홍보 같은 문의가 생기면 값을 더합니다. |
| 캠페인 코드 | `AdvertisementInquiry.campaignCode` | 같은 문의 유형 안의 이벤트. 페이지가 보내지 않고 서버가 진입 페이지로 정합니다. `/biz` 출시 기념 무료 홍보는 `launch_free_promo`입니다. |
| 홍보 채널 | `AdvertisementInquiryPromotionChannel` | 무료 홍보를 원하는 렛츠커리어 채널. 인스타그램, 직무별 오픈채팅방 6개, 추천 요청 |
| 광고 문의 처리 상태 | `AdvertisementInquiryStatus` | 접수(`RECEIVED`), 연락함(`CONTACTED`), 집행 완료(`EXECUTED`), 반려(`REJECTED`). 영업이 바꾸며 값 사이의 전이를 막지 않습니다. |
| 광고 문의 회원 구분 | `AdvertisementInquiryMemberType` | 기업 회원(`COMPANY`), 일반 회원(`USER`), 비회원(`GUEST`). 저장할 때 토큰으로 정한 문의 시점의 값입니다. |
| 광고 문의 회사명 입력 방식 | `AdvertisementInquiryCompanyNameSource` | 검색(`SEARCH`, 오공고 회사명 검색에서 고름), 직접 입력(`MANUAL`), 회원 정보(`MEMBER_PROFILE`, 기업 회원 정보로 채움). 클라이언트가 보낸 값을 그대로 저장하며 보내지 않으면 직접 입력입니다. 영업이 표기를 정리할 행을 고르는 표시입니다. |
| 광고 문의 유입 경로 | `AdvertisementInquiryInflowChannel` | 콜드메일·인스타그램·카카오톡·뉴스레터·기타 UTM·오공고 사이트·렛츠커리어·검색·외부 사이트·직접 접속. UTM과 referrer 원본으로 저장할 때 한 번 판정합니다. 회원의 유입 구분(`UserInflowSource`)과 다릅니다. |
| 이미지 | `ImageAsset` | 업로드한 이미지. 업로드 중 → 임시 → 첨부 순으로 바뀌고, 쓰이지 않으면 정리 작업이 지웁니다. 어디에 쓰이는지는 모르고, 쓰는 쪽(본문 이미지 노드, 프로필)이 이미지 식별자와 URL을 가집니다. |
| 이미지 용도 | `ImageAssetUsage` | 프로필(`ogonggo/profile`), 모집글(`ogonggo/side`), 취준고민(`ogonggo/concern`). 업로드할 때 받아 S3 폴더만 정하고, 어디에 붙일 수 있는지는 정하지 않습니다. |
| 지역 | `Region`, `SubRegion` | 시·도와 시·군·구. 행정 코드를 함께 가집니다. |
| 스케줄 작업 | `ScheduledJob` | 주기 작업의 실행 주기와 켜짐 여부. 운영자가 DB에서 바꿉니다. |
| 관리 목록 / 콘솔 목록 | `*ManagementSearchCondition`, `*ConsoleSearchCondition` | 게시 상태와 무관하게 콘텐츠를 관리할 때 쓰는 목록 조건. 관리자 콘솔이나 작성자 본인의 관리 화면에서 씁니다. |

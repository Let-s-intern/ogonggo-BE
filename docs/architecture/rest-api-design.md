# 오공고 REST API 설계

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-10-08
- 적용 범위: `ogonggo-api-user`, `ogonggo-api-admin`
- 예상 독자: 사용자·관리자 API를 개발하거나 연동하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 사용자 API와 관리자 API는 독립 애플리케이션이므로 같은 `/api/v1` 아래에서 각자의 리소스 계약을 제공하며, 관리자 경로에 `/admin`을 중복해서 넣지 않습니다.

```text
사용자 API base URL + /api/v1/jobs
관리자 API base URL + /api/v1/jobs
```

두 API는 호스트·포트·배포와 Security 설정으로 구분합니다. 단일 외부 도메인에서 경로 기반 라우팅이 필요해지면 Gateway가 외부 prefix를 처리하고 애플리케이션에는 전달하지 않습니다.

### 예외: 관리자 콘솔 API

관리자 콘솔 화면이 쓰는 API는 `/api/v1/admin` 아래에 둡니다: `/api/v1/admin/jobs`, `/api/v1/admin/content-reviews`.

- 결정일: 2026-09-14 / 리뷰 상태: 팀 리뷰 필요
- 배경: 콘솔 화면이 목 핸들러로 먼저 만들어졌고 그 경로가 `/api/v1/admin/**`입니다. 화면을 고치지 않고 붙이기로 했습니다.
- 크롤러의 `/api/v1/internal/**`과 인증 방식(내부 API 키, 관리자 토큰)이 달라 경로 접두사로 인가 규칙을 나눕니다.
- 콘솔 계약을 따라 `PATCH /api/v1/admin/jobs/{jobId}`가 노출·검수 상태와 내용을 부분 수정으로 함께 받고, 검수 판정은 `PATCH /api/v1/admin/content-reviews/{type}/{id}`로 둡니다. 4절의 명령별 엔드포인트 원칙과 다르며 같은 이유의 예외입니다.
- 영향 범위: 관리자 API의 콘솔 Controller와 인가 규칙. 사용자 API와 크롤러 경로는 바뀌지 않습니다.

## 2. URI

- API 버전은 경로의 `/api/v1`로 표현합니다.
- 리소스는 복수 명사를 사용합니다: `/jobs`, `/bootcamps`.
- 여러 단어는 kebab-case를 사용합니다: `/job-bookmarks`.
- Path Variable은 의미가 드러나는 이름을 사용합니다: `/{jobId}`.
- 끝의 `/`는 사용하지 않습니다.
- 필터·정렬·페이지 조건은 Query Parameter로 전달합니다.
- 요청 본문이 필요한 복잡한 검색만 `POST /search`를 허용하며 실제 요구가 생길 때 정의합니다.
- `/health`는 배포 환경용 운영 엔드포인트이므로 버전 경로와 성공 응답 포맷을 적용하지 않습니다.
- 사람이 아닌 내부 클라이언트 전용 경로는 `/api/v1/internal` 아래에 둡니다: `/api/v1/internal/jobs`, `/api/v1/internal/bootcamps`.

## 3. HTTP 메서드와 상태

| 작업 | 메서드 | 성공 상태 |
| --- | --- | ---: |
| 목록·상세 조회 | GET | 200 |
| 리소스 생성 | POST | 201 |
| 전체 교체 | PUT | 200 |
| 일부 수정 | PATCH | 200 |
| 소프트 삭제 | DELETE | 200 |
| 로그인·토큰·상태 전이 명령 | POST 또는 PATCH | 200 |

응답 본문은 [API 성공 응답 기준](api-response.md)을 따릅니다. 생성은 이후 작업에 필요한 식별자를 가능한 한 `data.id`로 반환합니다. 수정·삭제처럼 반환할 데이터가 없으면 200과 `data: null`을 사용하며 204는 사용하지 않습니다.

PUT은 리소스 전체 교체가 실제로 필요할 때만 추가합니다. 일부 필드 변경에 PUT을 사용하지 않습니다.

## 4. 상태 전이와 명령

CRUD만으로 의도가 불분명한 도메인 명령은 동사형 하위 경로를 허용합니다.

```text
POST /api/v1/jobs/{jobId}/publish
POST /api/v1/jobs/{jobId}/hide
POST /api/v1/jobs/{jobId}/close
```

하나의 `PATCH /status`가 모든 상태 값을 받도록 만들지 않습니다. 허용되는 행위와 전이 규칙이 다르면 명령별 엔드포인트와 Business Service 메서드로 구분합니다.

인증의 `/api/v1/auth/letscareer`, `/api/v1/auth/token`, `/api/v1/auth/signout`도 리소스 CRUD가 아닌 명령 API 예외로 유지합니다.

## 5. 멱등성과 재요청

- GET, PUT, DELETE는 같은 요청을 반복해도 목표 상태가 같도록 설계합니다.
- PATCH와 상태 전이는 기능별 전이 규칙을 따르며 반복 요청의 결과를 테스트합니다.
- 일반 POST 생성은 멱등성을 보장하지 않습니다.
- 결제나 외부 전송처럼 중복 실행 피해가 큰 기능에만 멱등 키를 도입하며 현재 공통 기반에는 추가하지 않습니다.
- 데이터 중복을 막아야 하면 도메인 검증과 DB 제약을 함께 사용합니다.

### 채용공고 북마크

```text
GET    /api/v1/job-bookmarks
POST   /api/v1/job-bookmarks/{jobId}
DELETE /api/v1/job-bookmarks/{jobId}
```

등록은 게시 중인 공고만 허용하고 성공 시 201을 반환합니다. 같은 사용자가 활성 북마크를 다시 등록하면 409 `JOB_BOOKMARK_ALREADY_EXISTS`로 응답합니다. 해제는 소프트 삭제하며 같은 요청을 반복해도 200으로 응답합니다. 해제한 공고를 다시 등록하면 기존 행을 복구합니다.

### 이동 기록

```text
POST /api/v1/jobs/{jobId}/source-url-clicks
POST /api/v1/bootcamps/{bootcampId}/source-url-clicks
```

외부 링크를 눌렀다는 사실을 남기는 기록이며 경로 이름은 기록하는 필드(`sourceUrl`, `applicationUrl`)를 따릅니다. 새 행이 생기지 않는 호출이 있어 201이 아니라 200과 `data: null`로 응답합니다.

### 내 정보

```text
GET    /api/v1/users/me
PUT    /api/v1/users/me/profile
PUT    /api/v1/users/me/company-profile/basic-info
PUT    /api/v1/users/me/company-profile/manager-info
PUT    /api/v1/users/me/notification-email
PUT    /api/v1/users/me/profile-image
DELETE /api/v1/users/me/profile-image
PATCH  /api/v1/users/me/password
```

`/api/v1/users/me`는 로그인한 사용자 자신을 가리키는 리소스이며, `/users/me/jobs`, `/users/me/bootcamps`, `/users/me/profile`이 그 하위에 있습니다. 프로필은 사용자마다 하나뿐인 단일 리소스여서 목록이 아니므로 단수 명사를 사용합니다. 복수 명사 규칙은 여러 항목을 담는 컬렉션에 적용합니다.

조회는 `/users/me` 응답의 `profile`에 함께 담고 별도 GET을 두지 않습니다. 한 화면에서 역할과 프로필을 함께 쓰므로 호출을 나눌 이유가 없습니다. 수정만 따로 여는 이유는 `/users/me` 응답에 `role`이나 `status`처럼 사용자가 바꿀 수 없는 값이 함께 있어 그대로 PUT의 대상이 될 수 없기 때문입니다. 기업 회원의 기업 정보도 같은 이유로 조회는 `/users/me`의 `companyProfile`에 담고 수정만 `/users/me/company-profile` 아래에 엽니다. 마이페이지가 기본 정보(기관명·로고)와 담당자 정보(이름·연락처·수신 이메일)를 서로 다른 버튼으로 저장하므로 `basic-info`와 `manager-info` 두 단일 리소스로 나누고, 각 폼의 값을 PUT으로 함께 교체합니다(2026-09-30, 팀 리뷰 필요. 이전에는 `PUT /users/me/company-profile` 하나가 모든 값을 교체했습니다). 로고는 `POST /api/v1/images`로 올린 이미지의 `logoImageId`로 받으며, 로고를 바꾸지 않을 때도 `/users/me`의 `companyProfile.logoImageId`를 그대로 보내야 합니다. 빼면 로고를 지웁니다. 기업 회원이 아니면 403 `COMPANY_ROLE_REQUIRED`, 정지·탈퇴 계정이면 403 `USER_SUSPENDED`·`USER_WITHDRAWN`입니다. 로그인 이메일과 비밀번호는 이 경로로 바꾸지 않습니다.

일반 회원의 오늘의 공고 수신 이메일은 값 하나짜리 단일 리소스로 보고 `PUT /users/me/notification-email`로 교체합니다. 빼거나 `null`이면 비웁니다. 비밀번호 변경은 기존 비밀번호 확인이 따르는 명령이라 조회 대상이 아니므로 `PATCH /users/me/password`로 두고 200과 `data: null`로 응답합니다. 두 경로의 소유권과 오류 규칙은 [사용자 인증 문서](authentication.md#오늘의-공고-수신-이메일)를 따릅니다.

일반 회원의 프로필 이미지도 값 하나짜리 단일 리소스로 보고 `PUT /users/me/profile-image`가 `{ "imageId": "..." }`로 교체하며, `DELETE`는 지워 렛츠커리어 이미지로 되돌립니다(2026-09-30, 팀 리뷰 필요). 파일은 게시글 이미지와 같은 `POST /api/v1/images`로 먼저 올리고 받은 식별자로 연결합니다. 같은 식별자를 다시 보내거나 지운 뒤 다시 지워도 200입니다. 소유권과 이미지 정리는 [사용자 인증 문서](authentication.md#프로필-이미지)를 따릅니다. 식별자를 경로에 넣어 남의 정보를 조회하는 `/users/{userId}`는 필요가 생길 때 정의합니다. 응답 계약은 [사용자 인증 문서](authentication.md#역할을-토큰에-담지-않는-이유)를 따릅니다.

### 부트캠프 북마크

```text
GET    /api/v1/bootcamp-bookmarks
POST   /api/v1/bootcamp-bookmarks/{bootcampId}
DELETE /api/v1/bootcamp-bookmarks/{bootcampId}
```

채용공고 북마크와 같은 규칙을 따릅니다. 등록은 지금 공개된 부트캠프만 허용하고, 중복 등록은 409 `BOOTCAMP_BOOKMARK_ALREADY_EXISTS`로 응답합니다.

### 지원·신청 관리 단계

```text
GET  /api/v1/job-bookmarks?applicationStatus={단계}&recruitmentStatus={모집 상태}&keyword={검색어}&sort=RECENTLY_SAVED
PUT  /api/v1/job-bookmarks/{jobId}/application-status

GET  /api/v1/bootcamp-bookmarks?applicationStatus={단계}&recruitmentStatus={모집 상태}&keyword={검색어}&sort=RECENTLY_SAVED
PUT  /api/v1/bootcamp-bookmarks/{bootcampId}/application-status

GET  /api/v1/recruitment-post-bookmarks?recruitmentStatus={모집 상태}&recruitmentType={유형}&keyword={검색어}&sort=RECENTLY_SAVED
GET  /api/v1/users/me/recruitment-post-applications?applicationStatus={단계}&recruitmentStatus={모집 상태}&recruitmentType={유형}&keyword={검색어}
POST /api/v1/recruitment-post-bookmarks/{postId}/prepare
POST /api/v1/recruitment-post-bookmarks/{postId}/cancel-preparation
```

- 결정일: 2026-09-21 / 리뷰 상태: 팀 리뷰 필요
- 마이페이지 지원·신청 관리 화면은 탭마다 단계가 다릅니다.

| 탭 | 단계 | 저장 위치 |
| --- | --- | --- |
| 채용공고 | 스크랩(`SCRAPPED`), 지원 준비 중(`PREPARING`), 지원 완료(`APPLIED`), 면접(`INTERVIEWING`), 합격(`PASSED`), 불합격(`FAILED`) | 북마크 행의 `application_status` |
| 교육·부트캠프 | 스크랩(`SCRAPPED`), 신청 전(`PREPARING`), 신청 완료(`APPLIED`), 활동 중(`IN_PROGRESS`), 활동 완료(`COMPLETED`) | 북마크 행의 `application_status` |
| 사이드·스터디 | 스크랩, 지원 준비 중(`PREPARING`), 지원 완료(`COMPLETED`), 활동 중(`IN_PROGRESS`), 활동 완료(`ENDED`) | 스크랩은 북마크, 나머지는 지원 이력 |

- 채용공고·부트캠프는 단계를 북마크 행이 가지며, 등록하거나 해제 후 다시 등록하면 스크랩에서 시작합니다. 각 단계 목록은 북마크 목록에 `applicationStatus`를 주어 조회하고, 단계별 건수는 그 응답의 전체 건수를 씁니다. 한 단계만 모아 보는 필터도 같은 `applicationStatus`를 씁니다.
- 사이드·스터디는 외부 연락처를 열면 생기는 지원 이력(LC-3309)이 이미 지원 준비 중 이후 단계를 가지므로 새로 저장하지 않습니다. 스크랩 칸은 북마크 목록, 나머지 칸은 지원 이력 목록을 씁니다. 지원 이력 단계 변경은 기존 `PATCH /api/v1/users/me/recruitment-post-applications/{postId}`를 씁니다.
- 마감 상태는 채용공고·부트캠프·사이드·스터디 모두 `recruitmentStatus`로 모집 중(`RECRUITING`)·모집 마감(`CLOSED`)을 고릅니다. 사용자 공개 목록(`GET /api/v1/jobs`, `GET /api/v1/bootcamps`)도 같은 파라미터를 받습니다. 부트캠프는 `DRAFT`를 보내면 400 `BAD_REQUEST`입니다. 관리자 부트캠프 목록은 응답 필드 이름을 따라 계속 `status`를 씁니다. 공고 검색은 목록의 `keyword`를 그대로 씁니다.
- 북마크 목록 정렬은 `sort`로 고르며 지금은 최근 저장순(`RECENTLY_SAVED`)만 있고 기본값입니다. 등록·재등록하거나 단계를 옮긴 시각이 최근인 순서입니다. 다른 정렬은 필요할 때 값을 추가합니다.
- 채용공고·부트캠프 단계에는 선후 관계가 없어 어느 단계에서든 다른 어느 단계로든 옮길 수 있습니다. 그래서 이동마다 명령 경로를 두지 않고 `PUT .../application-status`가 `{ "applicationStatus": "INTERVIEWING" }`처럼 목표 단계를 받습니다. 4절의 명령별 경로 원칙은 전이 규칙이 행위마다 다를 때를 위한 것이라, 전이 규칙이 없는 이 경우에는 적용하지 않습니다.
  - 결정일: 2026-09-22 / 리뷰 상태: 팀 리뷰 필요. 이전(2026-09-21)에는 스크랩과 지원 준비 중(부트캠프는 신청 전) 사이만 옮길 수 있어 `POST .../prepare`, `POST .../cancel-preparation`을 두었습니다. 화면에서 모든 단계를 서로 옮길 수 있게 정해져 두 경로를 없애고 하나로 바꿨습니다. 영향 범위는 채용공고·부트캠프 북마크 단계 이동 API이며 사이드·스터디는 바뀌지 않습니다.
  - 이미 목표 칸에 있으면 아무것도 바꾸지 않고 200으로 응답합니다. `applicationStatus`가 없거나 정의되지 않은 값이면 400 `BAD_REQUEST`입니다.
  - 활성 북마크가 없으면 404 `JOB_BOOKMARK_NOT_FOUND`·`BOOTCAMP_BOOKMARK_NOT_FOUND`입니다.
- 사이드·스터디의 스크랩 ↔ 지원 준비 중은 계속 `POST .../prepare`, `POST .../cancel-preparation`으로 옮깁니다. 활성 북마크가 없으면 404 `RECRUITMENT_POST_BOOKMARK_NOT_FOUND`, 허용되지 않는 이동은 409 `INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION`입니다.
- 사이드·스터디의 스크랩 → 지원 준비 중은 북마크를 해제하고 지원 준비 중 지원 이력을 만듭니다. 되돌리기는 지원 이력을 지우고 북마크가 없으면 다시 북마크합니다. 연락처를 열지 않고 만든 지원 이력은 옮긴 시각을 최초 접근 시각으로 기록합니다.
- 채용공고·부트캠프에서 이동한 북마크는 수정 일시가 갱신되어 해당 단계 목록의 맨 앞에 옵니다.
- 사이드·스터디에서 북마크한 모집글의 연락처를 열면 스크랩 칸과 지원 준비 중 칸에 함께 보입니다. 이때 한쪽을 정리할지는 **확인 필요**입니다.

### 추천 렛츠커리어 챌린지

```text
GET /api/v1/recommended-challenges
```

- 결정일: 2026-09-28 / 리뷰 상태: 팀 리뷰 필요
- 오공고 리소스가 아니라 렛츠커리어가 이 사용자에게 추천한 챌린지 모음이므로, `/challenges`가 아닌 `recommended-challenges`로 이름에 추천임을 드러냅니다. 오공고가 챌린지 목록·상세를 따로 열 계획은 없습니다.
- 응답 계약은 [API 성공 응답의 추천 렛츠커리어 챌린지](api-response.md#추천-렛츠커리어-챌린지)를 따릅니다.

### 채용공고별 렛츠커리어 콘텐츠 추천

```text
GET /api/v1/jobs/{jobId}/recommended-lets-career-contents
```

- 결정일: 2026-10-09 / 리뷰 상태: 팀 리뷰 필요
- 추천은 공고마다 다르므로 채용공고 하위에 둡니다. 오공고가 소유한 리소스가 아니라 렛츠커리어 콘텐츠 가운데 이 공고에 맞춰 고른 모음이라 `recommended-`로 추천임을 드러냅니다.
- 채용공고 조회와 같이 로그인 없이 엽니다(`GET /api/v1/jobs/**`).
- 공고 상세 응답에 싣지 않고 따로 둔 이유는 추천 계산이 실패해도 상세는 떠야 하고, 화면에서 따로 늦게 그려도 되기 때문입니다.
- 응답 계약은 [API 성공 응답의 채용공고별 렛츠커리어 콘텐츠 추천](api-response.md#채용공고별-렛츠커리어-콘텐츠-추천)을 따릅니다.

### 오늘의 공고

```text
GET /api/v1/jobs/today                        사용자 API, 로그인 선택
GET /api/v1/admin/jobs/today                  관리자 콘솔
PUT /api/v1/admin/jobs/today
```

- 결정일: 2026-09-30 / 리뷰 상태: 팀 리뷰 필요
- 운영자가 관리자 콘솔에서 직접 고른 채용공고 모음입니다. 항목이 채용공고이고 목록과 같은 카드로 보여 주므로 인기 공고(`/jobs/popular`)처럼 `/jobs` 아래에 둡니다.
- 날짜별로 두지 않고 목록 하나를 유지합니다. 운영자가 바꿀 때까지 같은 목록이 나오므로 경로와 요청에 날짜가 없습니다.
- 설정은 목록 전체를 한 번에 바꾸므로 PUT이며 `{ "jobIds": [7, 3] }`처럼 공고 식별자를 받습니다. 배열 순서가 노출 순서이고 개수 제한은 없습니다. 빈 배열은 오늘의 공고를 비웁니다. 같은 요청을 반복해도 결과가 같습니다.
- 한 건씩 넣고 빼는 경로(`POST`·`DELETE /jobs/today/{jobId}`)는 두지 않습니다. 순서까지 함께 정해야 해서 전체 교체 하나로 충분합니다.
- 응답 계약은 [API 성공 응답의 오늘의 공고](api-response.md#오늘의-공고)를 따릅니다.

### 관리자 콘솔 노출 일괄 변경

```text
PATCH /api/v1/admin/jobs/visibility
PATCH /api/v1/admin/bootcamps/visibility
PATCH /api/v1/admin/recruitment-posts/visibility
PATCH /api/v1/admin/concerns/visibility
```

- 결정일: 2026-10-01 / 리뷰 상태: 팀 리뷰 필요
- 운영자가 콘솔에서 검색한 뒤 여러 건을 골라 한 번에 노출·비노출로 바꿉니다. 단건 `PATCH /{jobId}`를 프런트가 반복 호출하기에는 건수가 많아 서버가 한 요청으로 받습니다.
- 본문은 `{ "ids": [7, 3], "visibility": "HIDDEN" }`입니다. `visibility`는 단건 수정과 같은 `VISIBLE`·`HIDDEN`이며, 고른 항목의 노출 상태 한 칸만 바꾸므로 PATCH입니다. 경로는 컬렉션의 `visibility` 속성을 가리키며 `/{jobId}`보다 먼저 매칭됩니다.
- 노출만 바꾸고 검수 상태·내용은 바꾸지 않습니다. 이미 요청한 노출인 항목은 건드리지 않고 성공으로 봅니다. 그래서 같은 요청을 반복해도 결과가 같고, 초안에 `HIDDEN`을 보내도 초안으로 남습니다. 같은 식별자가 여러 번 와도 거절하지 않고 한 번만 바꿉니다.
- 한 트랜잭션에서 모두 바꾸며, 하나라도 바꿀 수 없으면 아무것도 바꾸지 않습니다. 일부만 바뀌면 운영자가 어느 항목이 바뀌었는지 다시 찾아야 하기 때문입니다.
  - 없거나 삭제된 항목이 있으면 404 `JOB_NOT_FOUND`·`BOOTCAMP_NOT_FOUND`·`RECRUITMENT_POST_NOT_FOUND`·`CONCERN_NOT_FOUND`이며, 운영자가 골라낼 수 있게 `message` 끝에 그 식별자를 오름차순으로 담습니다: `일자리 공고를 찾을 수 없습니다. (id: 7, 999)`. 오류 응답의 세 필드 계약은 그대로 두었습니다([예외 처리 기준](error-handling.md#3-예외-분류와-응답)).
  - 승인 전 기업회원 콘텐츠를 `VISIBLE`로 바꾸려 하면 409 `REVIEW_NOT_APPROVED`입니다. 어느 항목 때문인지는 담지 않습니다.
- `ids`는 1건 이상 1000건 이하입니다. 콘솔 목록 한 페이지(최대 100건)를 여러 장 골라도 넉넉하고 한 트랜잭션의 잠금이 길어지지 않을 만큼으로 정했습니다. 비었거나 넘치거나 양수가 아닌 값이 있거나 `visibility`가 없으면 400 `BAD_REQUEST`입니다.
- 여러 요청이 같은 항목을 잠글 때 교착되지 않도록 식별자 순으로 잠급니다.
- 성공하면 200과 `data: null`로 응답합니다. 바뀐 항목은 목록을 다시 조회해 확인합니다.
- 사이드·스터디 모집글(결정일: 2026-10-02 / 리뷰 상태: 팀 리뷰 필요)은 공개(`PUBLISHED`)를 `VISIBLE`, 비공개(`HIDDEN`)를 `HIDDEN`으로 봅니다. 임시저장은 작성자만 보는 글이라 콘솔 목록에 없고, `ids`에 있으면 없는 모집글과 같이 404입니다. 운영자가 숨긴 모집글은 작성자의 내 모집글 관리에 비공개로 보이며, 작성자가 게시(`POST .../publish`)로 되돌릴 수 없어 400 `RECRUITMENT_POST_NOT_READY`입니다. 다시 내놓는 것은 운영자만 합니다. 숨긴 모집글을 작성자가 복사해 새로 게시하는 것을 막을지는 **확인 필요**입니다.
- 취준고민 고민글(결정일: 2026-10-08 / 리뷰 상태: 팀 리뷰 필요)은 게시 상태 대신 숨김 여부(`concerns.hidden`)를 두고, 숨기지 않은 글을 `VISIBLE`, 숨긴 글을 `HIDDEN`으로 봅니다. 숨긴 고민글은 사용자 API에서 없는 글과 같습니다([API 성공 응답의 취준고민](api-response.md#취준고민)).
- 검색 조건 전체를 받아 서버가 대상을 고르는 방식(`ids` 대신 필터)은 화면이 고른 항목만 바꾸는 요구와 달라 두지 않았습니다. 필요해지면 그때 정합니다.

### 서비스 개선 의견

```text
POST /api/v1/service-feedbacks                사용자 API, 로그인 선택
GET  /api/v1/admin/service-feedbacks          관리자 콘솔
```

- 결정일: 2026-09-28 / 리뷰 상태: 팀 리뷰 필요
- 사용자가 "이용 중 가장 만족스러운 점"(`satisfaction`)과 "아쉬운 점이나 개선됐으면 하는 점"(`improvement`)을 남깁니다. 제출할 때마다 새 행이 생기므로 201과 `data.id`로 응답합니다.
- 로그인 없이도 남길 수 있어 채용공고 조회처럼 토큰이 선택입니다. 토큰을 보내면 작성자(`user_id`)를 함께 기록하고, 토큰이 없거나 유효하지 않으면 작성자 없이 저장합니다.
- 한 사용자가 여러 번 남길 수 있어 유니크 제약을 두지 않고 409도 쓰지 않습니다.
- 수정·삭제 경로는 두지 않습니다. 운영자는 관리자 콘솔에서 목록을 읽기만 합니다.
- 응답 계약은 [API 성공 응답의 서비스 개선 의견](api-response.md#서비스-개선-의견)을 따릅니다.

### 크롤러 채용공고

```text
POST   /api/v1/internal/jobs
GET    /api/v1/internal/jobs?sourceUrl={원문 URL}
PUT    /api/v1/internal/jobs/{jobId}
DELETE /api/v1/internal/jobs/{jobId}
```

크롤러가 내부 API 키로 호출합니다. 소유자가 없는 수집 공고만 다루며, 기업회원 공고는 없는 공고와 같이 404로 응답합니다.

- 등록은 201과 `data.jobId`를 반환합니다. 같은 원문 URL의 미삭제 공고가 있으면 409 `JOB_ALREADY_EXISTS`입니다.
- 크롤러는 등록 응답의 식별자를 저장해 교체·삭제에 씁니다. 식별자를 잃었으면 `GET ?sourceUrl=`로 되찾습니다. 원문 URL을 경로 변수로 쓰지 않는 이유는 URL 안의 `/`·`#`이 경로와 섞이기 때문입니다.
- 교체는 다시 수집·분류한 값으로 공고 전체를 바꾸므로 PUT이며 200과 `data: null`로 응답합니다. 같은 값을 반복해 보내도 결과가 같습니다. 태그는 등록할 때만 받고 교체하지 않습니다.
- 삭제는 소프트 삭제이며 반복해도 200입니다. 직무별로 나뉘어 새 공고로 등록된 원래 공고를 지울 때 씁니다.
- 크롤러 공고는 검수를 거치지 않고 등록하면 곧바로 게시합니다. 교체는 게시 상태를 바꾸지 않습니다. [API 성공 응답의 검수와 노출](api-response.md#검수와-노출) 참고.

### 크롤러 공고 분석

```text
GET /api/v1/internal/jobs/analysis-targets?size={개수}
PUT /api/v1/internal/jobs/{jobId}/analysis
```

- 결정일: 2026-10-08 / 리뷰 상태: 팀 리뷰 필요
- 채용공고 상세의 공고 분석([API 성공 응답의 채용공고 공고 분석](api-response.md#채용공고-공고-분석))을 크롤러가 AI로 만듭니다. 서버에는 AI 클라이언트를 두지 않습니다. 크롤러에 분석 방법 편집·시험 화면과 비용 기록이 이미 있어서입니다.
- 크롤러 채용공고 API와 달리 **등록 경로를 가리지 않습니다.** 고용24·기업회원 공고도 같은 분석을 보여 줘야 하기 때문입니다. 인증은 같은 내부 API 키입니다.
- 대상 조회는 게시 중인 모집 중 공고 가운데 분석이 없거나 분석한 뒤 본문 해시가 바뀐 공고를 최근 것부터 `size`건(기본 50, 1 이상 200 이하) 줍니다. 항목은 크롤러 등록 요청과 같은 칸 이름의 본문과 `jobId`, `source`, `contentHash`입니다.
  - 매번 모든 공고의 해시를 세지 않도록, 분석하거나 본문을 확인했을 때의 공고 수정 일시를 `job_analyses.job_updated_at`에 남기고 공고 수정 일시가 그와 다른 공고만 다시 비교합니다. 본문 아닌 칸만 바뀐 공고(크롤러가 같은 내용으로 교체한 공고 등)는 확인한 것으로 남기고 대상에서 뺍니다. 그래서 이 GET은 확인 일시를 쓰는 부작용이 있습니다. 같은 요청을 반복해도 돌려주는 대상은 같습니다.
- 저장은 공고마다 하나인 분석을 통째로 바꾸므로 PUT이며 200과 `data: null`로 응답합니다. 같은 값을 다시 보내도 결과가 같습니다.
  - 요청의 `contentHash`가 지금 본문 해시와 다르면 409 `JOB_ANALYSIS_OUTDATED`입니다. 대상을 받은 뒤 본문이 바뀐 경우이며, 그 공고는 다음 대상 조회에 다시 나옵니다.
  - 없거나 지워진 공고는 404 `JOB_NOT_FOUND`입니다. 게시 상태는 보지 않습니다.
- 크롤러는 매일 고용24 수집(04:00)과 크롤러 매일 수집 뒤에 이 두 API로 분석합니다. 실행 시각은 크롤러 설정이 정합니다.

### 크롤러 렛츠커리어 콘텐츠 태그

```text
GET /api/v1/internal/lets-career-contents/tag-targets?size={개수}
PUT /api/v1/internal/lets-career-contents/{contentId}/tags
```

- 결정일: 2026-10-09 / 리뷰 상태: 팀 리뷰 필요
- 공고별 콘텐츠 추천([API 성공 응답](api-response.md#채용공고별-렛츠커리어-콘텐츠-추천))에 쓰는 태그를 크롤러가 AI로 붙입니다. 공고 분석과 같은 이유로 서버에는 AI 클라이언트를 두지 않습니다. 인증은 같은 내부 API 키입니다.
- 대상 조회는 렛츠커리어 목록에 있는 콘텐츠 가운데 태그가 없거나 태그한 뒤로 종류·분류·제목·설명·단서의 해시(`contentHash`)가 바뀐 것을 오래된 것부터 `size`건(기본 50, 1 이상 200 이하) 줍니다. 모집 기간만 바뀐 콘텐츠는 다시 태그하지 않습니다.
- 태그는 직군(`jobFields`)·직무(`jobRoles`)·준비 단계(`topics`)이며 콘텐츠마다 하나뿐이라 통째로 바꾸는 PUT입니다. 200과 `data: null`로 응답합니다. 직군·직무를 모두 비우면 어느 직무에나 맞는 콘텐츠로 봅니다.
  - 요청의 `contentHash`가 지금 해시와 다르면 409 `LETS_CAREER_CONTENT_TAGS_OUTDATED`입니다. 그 콘텐츠는 다음 대상 조회에 다시 나옵니다.
  - 없거나 렛츠커리어 목록에서 빠진 콘텐츠는 404 `LETS_CAREER_CONTENT_NOT_FOUND`입니다.

### 크롤러 부트캠프

```text
POST   /api/v1/internal/bootcamps
GET    /api/v1/internal/bootcamps?sourceUrl={원문 URL}
PUT    /api/v1/internal/bootcamps/{bootcampId}
DELETE /api/v1/internal/bootcamps/{bootcampId}
```

크롤러 채용공고와 같은 계약입니다. 소유자가 없고 원문 URL이 있는 수집 부트캠프만 다루며, 기업회원 부트캠프와 원문 URL이 없는 부트캠프는 없는 부트캠프와 같이 404 `BOOTCAMP_NOT_FOUND`로 응답합니다.

- 등록은 201과 `data.bootcampId`를 반환합니다. 같은 원문 URL의 미삭제 부트캠프가 있으면 409 `BOOTCAMP_ALREADY_EXISTS`입니다.
- 요청 본문의 선택 칸 `status`는 모집 상태이며 `RECRUITING` 또는 `CLOSED`만 받습니다. `DRAFT` 등 다른 값은 400 `BAD_REQUEST`입니다. 새싹처럼 운영 중이거나 과정이 끝난 과정도 모집 마감으로 보내 게시해 둡니다.
- 등록하면 게시(`PUBLISHED`) 상태로 곧바로 노출하며 검수는 거치지 않습니다. `status`가 없으면 모집 중(`RECRUITING`)이고, `CLOSED`면 모집 중으로 저장한 뒤 등록 시각으로 마감합니다(`closedAt` = 등록 시각).
- 교체는 PUT이며 200과 `data: null`로 응답합니다. 커리큘럼은 기존 것을 소프트 삭제하고 보낸 목록으로 바꾸며, 배열 순서가 노출 순서입니다. 게시 상태와 크롤러가 보내지 않는 공개 기간·파트너사는 그대로 둡니다.
- 요청 본문의 선택 칸 `logoUrl`은 운영 회사 로고이며 대표 이미지(`representativeImageUrl`)와 따로 저장합니다. 빈 문자열은 400이고, 값이 없으면 보내지 않습니다. 교체에 보내지 않으면 로고를 지웁니다.
- 교체에 `status`를 보내면 모집 상태를 그 값으로 맞춥니다. `RECRUITING`→`CLOSED`는 교체 시각으로 마감하고, `CLOSED`→`RECRUITING`은 마감 일시를 지우고 다시 모집 중으로 둡니다. 같으면 그대로이며, `status`가 없으면 모집 상태를 바꾸지 않습니다.
- 삭제는 소프트 삭제이며 반복해도 200입니다.
- 부트캠프 모집 상태는 저장된 값이라 모집 종료 일시가 지나도 저절로 `CLOSED`가 되지 않습니다. 자동 마감 처리는 **미정**입니다.

### 공지사항

```text
GET    /api/v1/announcements                          사용자 API, 로그인 없이 조회
GET    /api/v1/announcements/{announcementId}

GET    /api/v1/admin/announcements                    관리자 콘솔
POST   /api/v1/admin/announcements
GET    /api/v1/admin/announcements/{announcementId}
PATCH  /api/v1/admin/announcements/{announcementId}
DELETE /api/v1/admin/announcements/{announcementId}
```

- 결정일: 2026-09-22 / 리뷰 상태: 팀 리뷰 필요
- 2026-10-06에 경로와 코드·테이블 이름을 `notices`에서 `announcements`로 바꿨습니다(리뷰 상태: 팀 리뷰 필요). 앞으로 생길 알림(notification)과 이름이 헷갈리지 않게 하려는 것입니다. 에러 코드도 `ANNOUNCEMENT_NOT_FOUND`로 바뀌었습니다.
- 공지는 관리자만 작성하므로 쓰기 경로는 관리자 API에만 둡니다. 사용자 API에는 GET만 열고 나머지 메서드는 거부합니다.
- 노출·상단 고정도 콘솔의 다른 콘텐츠처럼 `PATCH`로 내용과 함께 부분 수정합니다. 1절의 관리자 콘솔 예외와 같은 이유입니다.
- 응답 계약은 [API 성공 응답의 공지사항](api-response.md#공지사항)을 따릅니다.

### 취준고민

```text
GET    /api/v1/concerns                                                        로그인 없이 조회
GET    /api/v1/concerns/popular                                                로그인 없이 조회
POST   /api/v1/concerns
GET    /api/v1/concerns/{concernId}                                            로그인 선택
PUT    /api/v1/concerns/{concernId}
DELETE /api/v1/concerns/{concernId}
GET    /api/v1/concerns/{concernId}/comments                                   로그인 선택
POST   /api/v1/concerns/{concernId}/comments
GET    /api/v1/concerns/{concernId}/comments/{commentId}/replies               로그인 선택
DELETE /api/v1/concerns/{concernId}/comments/{commentId}
PUT    /api/v1/concerns/{concernId}/comments/{commentId}/likes/me
DELETE /api/v1/concerns/{concernId}/comments/{commentId}/likes/me
GET    /api/v1/admin/concerns                                                  관리자 콘솔
GET    /api/v1/admin/concerns/{concernId}
PATCH  /api/v1/admin/concerns/visibility
```

- 결정일: 2026-10-08 / 리뷰 상태: 팀 리뷰 필요
- 수정은 카테고리·제목·본문 세 값을 모두 받아 바꾸므로 PUT이며 200과 `data: null`로 응답합니다.
- 지금 가장 핫한 고민은 기간·개수가 정해진 고민글 모음이라 인기 공고(`/jobs/popular`)처럼 `/concerns/popular`에 둡니다. 목록 API의 정렬로 대신하지 않는 이유는 최근 일주일이라는 기간 조건이 목록에는 없기 때문입니다.
- 답변과 답글은 같은 `comments` 컬렉션에 두고, 답글은 요청 본문의 `parentId`로 구분합니다. 모집글 댓글과 같은 구조입니다.
- 좋아요는 로그인한 사용자 자신의 표시 하나를 가리키는 단일 리소스(`likes/me`)로 보고, 북마크와 달리 PUT·DELETE로 둡니다. 버튼을 빠르게 두 번 눌러도 409 없이 같은 결과가 되게 하려는 것입니다.
- 답변·답글 삭제는 모집글 댓글과 같이 이미 지운 댓글이면 404입니다.
- 관리자 콘솔은 목록·상세 조회와 노출 일괄 변경([관리자 콘솔 노출 일괄 변경](#관리자-콘솔-노출-일괄-변경))만 둡니다. 운영자가 고민글을 고치거나 지우는 API와 답변 관리 API는 두지 않았습니다(2026-10-08).
- 응답 계약은 [API 성공 응답의 취준고민](api-response.md#취준고민)을 따릅니다.

### 관리자 회원 조회

```text
GET /api/v1/admin/general-members             일반 회원
GET /api/v1/admin/company-members             비즈니스(기업) 회원
```

- 결정일: 2026-09-28 / 리뷰 상태: 팀 리뷰 필요
- 콘솔 화면이 두 회원을 다른 메뉴·필터로 다루고 응답 항목도 달라 `/users?role=` 하나로 합치지 않고 역할별 컬렉션으로 나눕니다. 이름은 `UserRole`의 `USER`(일반 회원)·`COMPANY`(기업 회원)를 따릅니다.
- 목록만 둡니다. 목록 항목이 회원 정보 전체를 담아 상세 조회가 따로 필요하지 않고, 정지·역할 변경 같은 쓰기도 두지 않습니다.
- 응답 계약은 [API 성공 응답의 관리자 회원 조회](api-response.md#관리자-회원-조회)를 따릅니다.

### 고용24 Open API 조회

```text
GET /api/v1/admin/work24/{apiName}?{고용24 요청 파라미터}
```

- 결정일: 2026-09-27 / 리뷰 상태: 팀 리뷰 필요
- 운영자가 관리자 콘솔이나 Swagger에서 고용24 데이터를 확인하는 조회입니다. 서버가 인증키를 붙여 고용24를 대신 호출하고 저장하지 않습니다.
- `apiName`은 호출할 고용24 API로, 관리자 API의 `Work24Api` 이름을 kebab-case 소문자로 씁니다(`tomorrow-learning-card-courses`). 정의되지 않은 값은 `[apiName]` 400 `BAD_REQUEST`입니다.
- API가 24개이고 파라미터가 모두 달라 API마다 경로와 `@RequestParam`을 두지 않고, 경로 하나가 query 전체를 받아 이름 그대로 넘깁니다. [OpenAPI 명세](openapi.md)의 파라미터 이름 명시 원칙과 다른 예외이며, 파라미터 설명은 고용24 개발명세를 따릅니다. 수집·저장처럼 특정 API를 실제로 쓰게 되면 그 API는 이름 있는 파라미터와 전용 경로로 옮깁니다.
- 인증키(`authKey`), 응답 형식(`returnType`), 명세가 값을 고정한 파라미터는 서버가 채우므로 보내도 무시합니다. 값이 빈 파라미터는 보내지 않은 것으로 봅니다.
- 응답 계약은 [API 성공 응답의 고용24 Open API 조회](api-response.md#고용24-open-api-조회)를 따릅니다.

## 6. 현재 보류하는 항목

다음은 실제 기능과 클라이언트 요구가 생길 때 결정합니다.

- PUT 기반 전체 교체
- 일괄 생성·삭제와 노출 외 값의 일괄 수정. 관리자 콘솔 노출 일괄 변경만 [5절](#관리자-콘솔-노출-일괄-변경)에서 정했습니다
- 요청 본문을 사용하는 복잡한 검색
- 멱등 키의 헤더명, 저장소와 만료 시간
- 생성 응답의 `Location` 헤더

## 7. 검토했지만 선택하지 않은 대안

- **관리자 URI에 `/admin` 추가:** 렛츠커리어 단일 애플리케이션에서는 구분에 필요하지만 오공고는 관리자 API가 독립 배포되어 중복이므로 제외했습니다. 관리자 콘솔 API만 1절의 예외를 따릅니다.
- **렛츠커리어 URI를 그대로 복제:** 단수·복수와 관리자 prefix 위치가 혼재해 외부 동작만 참고하고 이름 규칙은 일관되게 정리했습니다.
- **모든 상태 변경을 `PATCH /status`로 통합:** 서로 다른 명령과 허용 조건이 하나의 입력에 섞이므로 제외했습니다.
- **삭제 성공에 204 사용:** 오공고의 공통 성공 응답 계약과 맞지 않아 200을 사용합니다.
- **모든 생성에 멱등 키 적용:** 현재 중복 실행 위험보다 구현·운영 비용이 커 필요한 기능에만 적용합니다.

API 버전 변경은 기존 클라이언트가 호환되지 않는 외부 계약 변경이 있을 때만 검토합니다. 내부 리팩터링이나 응답 필드 추가 없이 구현만 바뀌는 경우에는 버전을 올리지 않습니다.

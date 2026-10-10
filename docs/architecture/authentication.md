# 오공고 사용자 인증과 렛츠커리어 연동

- 상태: Accepted
- 결정일: 2026-08-27
- 최종 변경일: 2026-10-04
- 적용 범위: `ogonggo-api-user`, `ogonggo-core`, `lets-career-server`
- 예상 독자: 오공고 서버와 클라이언트를 개발·리뷰하는 팀원
- 리뷰 상태: 팀 리뷰 필요

## 1. 먼저 알아야 할 결정

> 오공고 계정은 두 종류다. 일반 회원은 렛츠커리어 로그인을 교환해 만들고, 기업 회원은 오공고가 직접 인증한다.
> 어느 쪽이든 로그인 이후의 모든 요청은 오공고가 발급한 같은 토큰으로 처리한다.

| 계정 | 인증 소유 | 진입점 | `users` 컬럼 |
| --- | --- | --- | --- |
| 일반 회원 | 렛츠커리어 | `POST /api/v1/auth/letscareer` | `letscareer_user_id` |
| 기업 회원 | 오공고 | `POST /api/v1/auth/company/signup`·`/signin` | `email`, `password` |

두 계정은 서로 무관하며 같은 사람이 양쪽을 따로 가질 수 있습니다. `users` 행은 둘 중 하나의 형태만 가지므로 `User` 엔티티는 생성자를 감추고 `ofLetsCareer`, `ofCompany` 팩토리만 노출해 잘못된 조합을 막습니다.

### 일반 회원: 렛츠커리어 토큰 교환

> 렛츠커리어 액세스 토큰은 오공고 로그인 순간에만 사용하고, 이후 모든 요청은 오공고가 발급한 토큰으로 처리한다.

```text
FE ──렛츠커리어 로그인(기존 OAuth2)──> 렛츠커리어
FE ──LC-access──> 오공고  ──X-Internal-Api-Key──> 렛츠커리어 /api/v1/internal/auth/verify
                     │                              └ userId, 프로필, updatedAt
                     ├ letscareer_user_id 로 계정 조회 또는 생성
                     ├ 프로필 동기화
                     └ OG-access / OG-refresh 발급
FE ──OG-access──> 오공고 (이후 렛츠커리어를 호출하지 않는다)
```

이 결정에서 가장 중요한 규칙은 다음 네 가지입니다.

1. 두 서비스는 JWT 서명 시크릿을 공유하지 않습니다.
2. 일반 회원은 `users.letscareer_user_id`로 렛츠커리어 계정과 1:1 대응합니다.
3. 일반 회원 계정은 별도 가입 API 없이 최초 토큰 교환 시점에 생성합니다.
4. 로그인 이후 렛츠커리어 장애는 오공고 사용자 요청에 영향을 주지 않습니다.

로그인 이후 렛츠커리어를 부르는 예외는 두 가지입니다.

1. 렛츠커리어 콘텐츠를 오공고 화면에 보여줄 때(지금은 [추천 챌린지](api-response.md#추천-렛츠커리어-챌린지)). 인증에는 쓰지 않고, 실패하면 빈 값으로 응답해 4번 규칙을 지킵니다.
2. 일반 회원이 [비밀번호를 바꿀 때](#비밀번호-변경). 비밀번호는 렛츠커리어에만 있으므로 전달할 수밖에 없습니다. 사용자가 직접 요청한 변경이라 실패를 숨기지 않고 503 `LETSCAREER_UNAVAILABLE`로 알리며, 이 요청 하나만 실패하고 다른 요청에는 영향이 없습니다.

## 2. 왜 이렇게 나누는가

렛츠커리어 액세스 토큰을 오공고가 직접 검증하려면 HS512 시크릿을 공유해야 합니다. 그러면 오공고 서버가 렛츠커리어 관리자 토큰을 위조할 수 있고, 한쪽이 유출되면 두 서비스가 함께 뚫리며, 시크릿 교체에 두 서버의 동시 배포가 필요합니다. 또한 오공고의 `UserStatus`를 렛츠커리어 토큰에 담을 수 없어 사용자 상태를 오공고가 독립적으로 관리할 수 없습니다.

토큰을 교환하면 신뢰 경계가 로그인 순간 한 번으로 좁아집니다. 오공고는 자신이 서명한 토큰만 해석하고, 사용자 상태와 권한을 스스로 소유합니다. 렛츠커리어가 응답하지 않아도 이미 로그인한 사용자는 영향을 받지 않으며, 새 로그인만 막힙니다.

프로필을 복제해 보관하는 이유도 같습니다. 화면에 이름이나 이메일을 표시할 때마다 렛츠커리어를 호출하면 조회 경로가 외부 서버 가용성에 묶입니다. `user_profiles`는 렛츠커리어 값의 사본이며 `letscareer_updated_at`이 바뀐 로그인에서만 갱신합니다.

## 3. 토큰과 수명

| 토큰 | 발급자 | 용도 | 기본 수명 |
| --- | --- | --- | --- |
| `LC-access` | 렛츠커리어 | 렛츠커리어 API 호출, 오공고 로그인 교환권 | 렛츠커리어 설정 |
| `LC-refresh` | 렛츠커리어 | `LC-access` 재발급 | 렛츠커리어 설정 |
| `OG-access` | 오공고 | 오공고 API 호출 | 30분 |
| `OG-refresh` | 오공고 | `OG-access` 재발급 | 14일 |

`OG-refresh`는 사용자당 하나만 Redis에 보관합니다(`ogonggo:refresh:{userId}`). 새로 로그인하면 이전 값이 덮어써져 무효가 되고, 로그아웃하면 삭제됩니다.

**이미 발급된 `OG-access`는 로그아웃해도 만료까지 유효합니다.** 액세스 토큰 블랙리스트를 두지 않고 수명을 30분으로 짧게 잡아 이 창을 좁히는 방식이며, 렛츠커리어의 로그아웃과 같은 구조입니다.

## 4. 분기별 흐름

### 첫 로그인

`readByLetsCareerUserId`가 `null`이면 `UserAppender.append`로 계정을 만들고 `isNewUser: true`로 응답합니다. 클라이언트는 이 값으로 온보딩 화면을 결정합니다.

계정·프로필 저장 트랜잭션 안에서 `UserSignedUpEvent`를 발행합니다. 커밋 후 리스너가 가입 알림톡 요청을 준비하고, NHN HTTP 호출은 `alimTalkTaskExecutor` 전용 풀에서 처리합니다. 가입 요청이 NHN 응답을 기다리지 않도록 분리한 구조이며, 롤백·재로그인·토큰 재발급·기업회원 가입에는 발송하지 않습니다.

수신번호는 `010`으로 시작하는 숫자 11자리 또는 `010-1234-5678` 형식만 받고, 하이픈을 제거해 전송합니다. 번호 오류나 필수 정보 누락은 사유 코드와 사용자 ID만 기록하고 알림을 생략합니다. 가입일은 한국 시간으로 저장된 `joinedAt`의 날짜를 `yyyy-MM-dd`로 사용합니다.

알림 실패로 가입을 되돌리지 않습니다. 메모리 이벤트·큐이므로 프로세스 종료나 실행기 포화로 인한 드문 유실을 허용하며, 가입 알림에는 스케줄러·영속 재시도·재로그인 시 보충 발송을 두지 않습니다. 이 정책은 2026-10-04 사용자 합의 사항이며 구현은 팀 리뷰 대상입니다.

### 재로그인

계정이 있으면 상태를 확인한 뒤 프로필만 동기화합니다. `letscareer_updated_at`이 이전 동기화 값과 같으면 UPDATE 하지 않습니다.

### 평상시 요청

`UserAuthenticationFilter`가 `OG-access`의 서명과 만료, `type=access`를 검증하고 `SecurityContext`에 사용자 식별자를 넣습니다. 렛츠커리어를 호출하지 않습니다.

### 액세스 토큰 재발급

`POST /api/v1/auth/token`은 `OG-refresh`의 서명·종류를 검증하고, Redis에 보관된 값과 일치하는지 확인한 뒤, 오공고 사용자 상태를 다시 확인해 새 `OG-access`를 발급합니다. **렛츠커리어를 재검증하지 않습니다.**

### 리프레시 토큰 만료

클라이언트는 보관 중인 `LC-access`로 교환을 다시 시도합니다. 그것도 만료됐다면 `LC-refresh`로 갱신하고, 갱신도 실패하면 렛츠커리어 로그인 화면으로 보냅니다.

### 로그아웃

`POST /api/v1/auth/signout`은 Redis의 `OG-refresh`만 삭제합니다. 렛츠커리어 세션은 그대로 유지됩니다.

모든 인증 엔드포인트의 성공 응답은 [API 성공 응답 기준](api-response.md)의 `SuccessResponse` 계약을 따릅니다. 로그아웃은 반환할 값이 없으므로 `data`가 `null`인 200으로 응답합니다.

### 실패

| 상황 | 응답 |
| --- | --- |
| `LC-access` 위조·만료, 렛츠커리어 사용자 없음 | 401 `INVALID_LETSCAREER_TOKEN` |
| 렛츠커리어 응답 없음, 내부 API 키 거부 | 503 `LETSCAREER_UNAVAILABLE` |
| `OG-access` 위조·만료 | 401 `UNAUTHORIZED` |
| 리프레시 자리에 액세스 토큰 전달 | 400 `NOT_REFRESH_TOKEN` |
| 로그아웃되었거나 교체된 리프레시 토큰 | 401 `EXPIRED_REFRESH_TOKEN` |
| 정지·탈퇴한 오공고 사용자 | 403 `USER_SUSPENDED`, `USER_WITHDRAWN` |
| 같은 사용자의 최초 로그인 동시 요청 | 409 `USER_ALREADY_EXISTS` (재시도하면 성공) |

마지막 행은 `letscareer_user_id` 유니크 제약이 걸린 경우입니다. 제약 위반 이후에는 트랜잭션이 롤백 대상이 되어 같은 트랜잭션에서 재조회할 수 없으므로, 같은 요청을 다시 보내도록 재시도 가능한 409로 변환합니다.

## 4-1. 로그인 없이 여는 조회 API

> 콘텐츠를 보는 데는 로그인을 요구하지 않는다. 로그인은 그 사람이 누구인지 응답에 담아야 할 때만 요구한다.

채용공고와 부트캠프의 조회 경로는 비로그인 사용자에게 열려 있습니다. 검색 유입과 공유 링크로 들어온 사람이 로그인 화면부터 만나지 않게 하기 위해서입니다.

| 경로 | 로그인 | 비고 |
| --- | --- | --- |
| `GET /api/v1/jobs`, `/api/v1/jobs/{jobId}` | 선택 | 토큰이 있으면 `bookmarked`가 채워지고, 없으면 항상 `false` |
| `GET /api/v1/jobs/calendar` | 선택 | 토큰이 있으면 `bookmarked`가 채워지고, 없으면 항상 `false`. `bookmarkedOnly=true`는 토큰이 없으면 401 |
| `GET /api/v1/bootcamps`, `/api/v1/bootcamps/{bootcampId}` | 선택 | 토큰이 있으면 `bookmarked`가 채워지고, 없으면 항상 `false` |
| `GET /api/v1/recommended-challenges` | 선택 | 토큰이 있으면 그 사용자의 렛츠커리어 계정을 추천에 넘기고, 없거나 기업 회원이면 넘기지 않는다 |
| `POST /api/v1/jobs/{jobId}/source-url-clicks` | 필수 | `source_url_clicks.user_id`가 NOT NULL이다 |
| `POST /api/v1/bootcamps/{bootcampId}/source-url-clicks` | 필수 | `source_url_clicks.user_id`가 NOT NULL이다 |
| `/api/v1/job-bookmarks/**`, `/api/v1/bootcamp-bookmarks/**` | 필수 | 북마크는 사용자별 상태다 |
| `/api/v1/users/me/bootcamps/**` | 필수 | 기업 회원이 자기 부트캠프를 관리한다 |
| `GET /api/v1/users/me` | 필수 | 자기 역할과 프로필을 읽는다 |
| `PUT /api/v1/users/me/profile` | 필수 | 자기 학력과 희망 조건을 고친다 |
| `PUT /api/v1/users/me/company-profile/basic-info` | 필수 | 기업 회원이 자기 기본 정보(기관명·로고)를 고친다 |
| `PUT /api/v1/users/me/company-profile/manager-info` | 필수 | 기업 회원이 자기 담당자 정보(이름·연락처·수신 이메일)를 고친다 |
| `PUT /api/v1/users/me/notification-email` | 필수 | 일반 회원이 오늘의 공고 수신 이메일을 고친다 |
| `PATCH /api/v1/users/me/password` | 필수 | 자기 비밀번호를 바꾼다 |

`anyRequest().denyAll()`은 그대로 둡니다. 여는 경로는 메서드와 함께 하나씩 명시하며, 목록이 아닌 것은 열리지 않습니다. 브라우저 preflight(`OPTIONS`)만 예외로, 인가 규칙 첫 줄의 `CorsUtils::isPreFlightRequest`가 먼저 허용합니다([브라우저 CORS 허용 오리진](#7-2-브라우저-cors-허용-오리진) 참고).

### 토큰이 선택인 경로의 주의점

`UserAuthenticationFilter`는 토큰이 유효하지 않으면 `SecurityContext`를 비우고 요청을 그대로 통과시킵니다. 이 동작과 `permitAll`이 만나면 **만료·위조 토큰으로 조회해도 401이 아니라 비로그인 응답(200, `bookmarked: false`)이 나갑니다.**

이는 의도한 동작입니다. 조회 화면이 토큰 만료로 비는 것보다 북마크 표시만 빠진 채로 보이는 편이 낫습니다. 대신 클라이언트는 조회 응답의 401로 재발급 시점을 알 수 없으므로, 재발급은 인증이 필수인 경로(북마크, 원문 이동 기록)의 401로 판단해야 합니다.

Business Service는 `userId: Long?`를 받고 `null`이면 북마크 저장소를 조회하지 않습니다. Presentation의 `@AuthenticationPrincipal`도 같은 경로에서만 nullable로 선언합니다. 인증이 필수인 경로는 `Long`을 그대로 유지해 실수로 익명 요청을 받지 않게 합니다.

## 5. 사용자 역할과 기업 회원

**오공고 역할과 기업 정보의 기준 시스템은 오공고**입니다. 렛츠커리어 role(`ADMIN`/`USER`)과는 별개 축이라 동기화하지 않습니다. 렛츠커리어에서 `USER`인 사람이 오공고에서 `COMPANY`일 수 있습니다.

| 역할 | 의미 | 부여 시점 |
| --- | --- | --- |
| `USER` | 일반 회원 | 렛츠커리어 최초 토큰 교환으로 계정을 만들 때 |
| `COMPANY` | 기업 회원 | 기업 회원가입 시점. 전환 절차 없이 처음부터 부여한다 |
| `ADMIN` | 관리자 회원 | 운영자가 DB에서 직접 부여한다. 관리자 콘솔 API에서만 쓴다([7-3](#7-3-관리자-콘솔-인증)) |

두 시스템의 enum은 코드 번호가 어긋납니다(렛츠커리어 `USER=2`, 오공고 `USER=1`). 역할을 숫자로 주고받지 않습니다.

### 기업 회원가입과 로그인

기업 회원은 렛츠커리어를 거치지 않습니다. 오공고가 이메일과 비밀번호를 직접 소유하는 유일한 지점입니다.

```text
POST /api/v1/auth/company/signup
{ "email": "...", "password": "...", "organizationName": "...", "managerName": "..." }
→ 201 { "accessToken": "...", "refreshToken": "..." }

POST /api/v1/auth/company/signin
{ "email": "...", "password": "..." }
→ 200 { "accessToken": "...", "refreshToken": "..." }
```

**승인 절차와 이메일 인증이 없습니다.** 가입 요청 시점에 계정과 `company_profiles`를 한 트랜잭션에서 만들고 바로 세션을 발급합니다. 가입 때 받는 기업 정보는 기관명과 담당자 이름 두 가지입니다. 기업 로고는 가입 후 `PUT /api/v1/users/me/company-profile/basic-info`에서, 담당자 연락처와 정보 수신용 이메일은 `PUT /api/v1/users/me/company-profile/manager-info`에서 선택 값으로 채웁니다.

| 상황 | 응답 |
| --- | --- |
| 가입 성공 | 201 |
| 이미 쓰는 이메일 | 409 `EMAIL_ALREADY_EXISTS` |
| 이메일 형식·비밀번호 길이 위반 | 400 `BAD_REQUEST` (`[email]`, `[password]`) |
| 로그인 자격증명 불일치 | 401 `INVALID_COMPANY_CREDENTIALS` |
| 정지·탈퇴한 계정 | 403 `USER_SUSPENDED`, `USER_WITHDRAWN` |

이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해 응답하지 않습니다. 구분하면 이 엔드포인트가 계정 존재 확인 수단이 됩니다.

비밀번호는 BCrypt로 `ogonggo-api-user`에서 인코딩하고 core에는 인코딩된 값만 넘깁니다. core는 인코딩 방식을 알지 않습니다. 이메일 중복은 사전 조회 대신 유니크 제약으로 판정합니다. 조회와 저장 사이에 같은 이메일이 들어오는 경쟁 상태를 조회로는 막을 수 없기 때문입니다.

### 학력과 희망 조건

> 렛츠커리어와 오공고 양쪽에서 고칠 수 있고, 나중에 고친 쪽의 값으로 맞춘다. (2026-09-29 결정)

```text
GET /api/v1/users/me            profile 안에 함께 담긴다
PUT /api/v1/users/me/profile    사용자가 고칠 수 있는 값만 교체한다
```

| 항목 | 값 | 소유 |
| --- | --- | --- |
| 이름·가입 이메일·휴대폰 번호·가입 경로·닉네임 | `name`, `email`, `phoneNum`, `authProvider`, `nickname` | 렛츠커리어. 로그인마다 `sync`가 갱신하고 오공고에서는 바꾸지 않는다 |
| 프로필 이미지 | `profileImageUrl` | 렛츠커리어 이미지를 로그인마다 복제하고, 오공고에서 바꾼 이미지가 있으면 그것을 먼저 보인다. [프로필 이미지](#프로필-이미지) 참고 |
| 학력 | `university`, `major`, `grade` | 양쪽. 나중에 고친 쪽이 이긴다 |
| 희망 조건 | `wishField`, `wishJob`, `wishIndustry`, `wishEmploymentType`, `wishCompany` | 양쪽. 나중에 고친 쪽이 이긴다 |
| 오늘의 공고 수신 이메일 | `notificationEmail` | 오공고. 사용자가 고친다 |

**같은 `user_profiles` 테이블에 두되 무엇이 무엇을 덮어쓰는지는 코드로 못 박아 둡니다.** `UserProfile.sync`는 렛츠커리어에서 복제하는 여섯 값만 건드리고, `UserProfile.replaceJobInfo`는 학력과 희망 조건 여덟 값과 그 수정 일시만, `UserProfile.changeNotificationEmail`은 수신 이메일만, `UserProfile.changeOgonggoProfileImage`·`removeOgonggoProfileImage`는 오공고 프로필 이미지 두 칸만 건드립니다. 한쪽 메서드에 다른 쪽 필드를 추가하지 않습니다.

`grade`는 렛츠커리어의 `UserGrade`와 값과 `code`를 맞춰 두었습니다. 희망 조건 다섯 값은 렛츠커리어가 자유 문자열로 다루므로 오공고도 형식을 해석하지 않고 그대로 보관합니다.

#### 선후 판정

양쪽이 학력·희망 조건만의 최종 수정 일시를 따로 갖습니다. 렛츠커리어의 `updatedAt`(`last_modified_date`)은 전화번호·비밀번호를 바꿔도 바뀌어 쓸 수 없습니다.

| 서버 | 칼럼 | 채워지는 때 |
| --- | --- | --- |
| 오공고 | `user_profiles.job_info_updated_at` | 오공고에서 고친 시각. 렛츠커리어 값을 받으면 렛츠커리어의 일시를 그대로 남긴다 |
| 렛츠커리어 | `user.job_profile_updated_at` | 렛츠커리어 마이페이지·어드민·가입 추가 정보에서 여덟 값이 실제로 바뀐 시각. 오공고 값을 받으면 오공고의 일시를 그대로 남긴다 |

받는 쪽은 보낸 쪽 일시가 **자기 일시보다 나중일 때만** 반영합니다. 같은 일시는 이미 받은 수정이라 반영하지 않으므로, 받은 값을 다시 돌려보내도 되돌아오는 고리가 생기지 않습니다. 자기 일시가 없으면(한 번도 고친 적 없는 과거 계정) 받고, 보낸 쪽 일시가 없으면 자기 일시도 없을 때만 받습니다. 두 서버 모두 KST로 떠서 일시를 그대로 비교하며, 몇 초 차이로 양쪽에서 고친 경우 두 서버의 시계 오차만큼 판정이 뒤집힐 수 있음을 감수합니다.

#### 전달: 양쪽 아웃박스

```text
오공고  PUT /users/me/profile ─┬ user_profiles 수정 ─┐ 한 트랜잭션
                               └ letscareer_job_profile_outbox 적재 ─┘
        letsCareerJobProfileSync(30초) → 렛츠커리어 PUT /api/v1/internal/users/{userId}/job-profile

렛츠커리어 여덟 값 수정 ─┬ user 수정 ─┐ 한 트랜잭션
                        └ ogonggo_job_profile_outbox 적재 ─┘
        ogonggoJobProfileSyncJob(30초) → 오공고 PUT /api/v1/internal/letscareer-users/{letsCareerUserId}/job-profile
```

- 아웃박스는 사용자당 한 행입니다. 여러 번 고쳐도 적재 일시만 갱신하고, 보낼 때 그 시점의 최신 값 전체와 수정 일시를 읽어 보냅니다. 순서가 바뀌거나 같은 요청이 여러 번 가도 받는 쪽 선후 판정 때문에 결과가 같습니다.
- 성공하면 **적재 일시가 그대로일 때만** 보낸 일시(`sent_at`)를 남깁니다. 행은 지우지 않으며, 다시 고치면 보낸 일시를 비워 다시 보낼 대상으로 되돌립니다. 보내는 사이 다시 고쳤으면 보낸 것으로 표시하지 않아 다음 주기에 새 값을 보냅니다.
- 실패하면 실패 횟수를 올리고 다음 주기에 다시 보냅니다. 계속 실패하는 행이 다른 행을 막지 않도록 실패가 적은 순으로 100건씩 보냅니다.
- 상대 호출은 트랜잭션 밖에서 하므로 상대 장애가 사용자의 수정 요청을 실패시키지 않습니다.
- 오공고는 렛츠커리어 계정이 있는 사용자만 적재합니다. 렛츠커리어는 오공고 계정 여부를 모르므로 모두 적재하고, 오공고는 계정이 없는 렛츠커리어 사용자를 `applied: false`로 무시합니다. 그 사람이 나중에 처음 로그인하면 최신 값을 복제합니다.
- 렛츠커리어에서 사용자가 사라졌으면(404) 보낼 곳이 없으므로 보낸 것으로 봅니다.
- 두 서버는 같은 내부 API 키(`ogonggo.letscareer.internal-api-key` = 렛츠커리어 `spring.security.internal.api-key`)로 서로를 인증합니다. 렛츠커리어는 오공고 사용자 API 주소를 `ogonggo.base-url`로 받으며, 비어 있으면 전송하지 않고 경고만 남깁니다.

두 내부 API 모두 여덟 값을 **전체 교체**합니다. null이면 비웁니다. 렛츠커리어의 기존 사용자 수정 API는 null이면 값을 유지하지만, 오공고 PUT이 보내지 않은 값을 비우므로 그 결과를 그대로 옮기기 위해서입니다.

#### 최초 가입

통합 로그인을 쓰는 이상 같은 정보를 두 번 입력하게 하지 않기 위해, 계정을 새로 만들 때 렛츠커리어의 값과 수정 일시를 한 번 가져옵니다. 이후 변경은 위의 아웃박스로 오갑니다.

```text
POST /api/v1/internal/auth/verify                    매 로그인
GET  /api/v1/internal/users/{userId}/job-profile     최초 가입 시 1회 (updatedAt 포함)
```

매 로그인 호출인 `verify`에 이 값들을 싣지 않고 계정을 새로 만들 때만 별도 내부 API를 한 번 호출합니다. 재로그인마다 쓰지도 않을 개인정보를 실어 나르지 않기 위한 것입니다.

렛츠커리어 호출은 로그인 트랜잭션 밖에서 합니다. 그 호출이 실패해도 가입은 성공으로 둡니다. 학력과 희망 조건은 로그인의 성공 조건이 아니고, 비어 있으면 사용자가 오공고에서 직접 입력하면 됩니다. 오공고가 모르는 `grade` 값이 오면 그 값만 비우고 나머지는 저장합니다.

PUT은 여덟 값을 함께 교체하며 보내지 않은 값은 비웁니다. 아직 입력한 적이 없어도 조회는 404가 아니라 값이 `null`인 200으로 응답합니다.

### 오늘의 공고 수신 이메일

```text
PUT /api/v1/users/me/notification-email   { "notificationEmail": "me@example.com" }
```

렛츠커리어 가입 이메일과 따로 두는 오공고 전용 값이라 `user_profiles.notification_email`에 두고 렛츠커리어에 보내지 않습니다. 빼거나 `null`로 보내면 비웁니다. 기업 회원은 기업 정보의 `notificationEmail`을 쓰므로 이 경로는 403 `GENERAL_MEMBER_REQUIRED`로 막습니다.

휴대폰 번호와 가입 경로(`letscareer_auth_provider`)는 마이페이지용으로 `verify` 응답에 실어 로그인마다 복제합니다. 가입 경로는 렛츠커리어에서 바뀌지 않는 값이라 첫 로그인에 저장된 값이 그대로 유지되며, 학력처럼 최초 1회 호출(`job-profile`)에 싣지 않은 이유는 그 호출이 실패하면 값을 다시 받을 기회가 없기 때문입니다. 두 값은 복제 대상에 나중에 추가되었으므로, `letscareer_updated_at`이 같아도 둘 중 하나가 다르면 한 번 갱신해 기존 행을 채웁니다. 렛츠커리어가 오공고가 모르는 가입 경로를 보내면 그 값만 비우고 로그인은 진행합니다.

### 프로필 이미지

> 오공고에서 바꾼 이미지는 오공고가 소유하고 렛츠커리어에 보내지 않는다. (2026-09-30 결정, 팀 리뷰 필요)

```text
POST   /api/v1/images                     파일을 올리고 imageId를 받는다
PUT    /api/v1/users/me/profile-image     { "imageId": "..." }로 바꾼다
DELETE /api/v1/users/me/profile-image     지워 렛츠커리어 이미지로 되돌린다
```

렛츠커리어에도 프로필 이미지가 있고 `verify` 응답의 `profileImageUrl`을 로그인마다 `user_profiles.profile_image_url`에 복제합니다. 이 칸을 오공고에서 고치면 렛츠커리어 프로필이 바뀐 뒤 다시 로그인할 때 덮어써지므로, 오공고에서 바꾼 이미지는 `ogonggo_profile_image_id`·`ogonggo_profile_image_url`에 따로 둡니다. 조회(`GET /users/me`의 `profile.profileImageUrl`, 모집글·댓글 작성자, 관리자 회원 목록)는 오공고 이미지가 있으면 그것을, 없으면 렛츠커리어 이미지를 보입니다. 렛츠커리어 쪽 이미지를 바꾸는 내부 API는 없어 렛츠커리어 화면에는 반영되지 않습니다.

- 일반 회원만 씁니다. 기업 회원은 403 `GENERAL_MEMBER_REQUIRED`입니다. 기업 회원의 프로필 이미지는 기업 로고이며, 기본 정보 수정(`PUT /users/me/company-profile/basic-info`)의 `logoImageId`로 같은 방식으로 연결합니다(`company_profiles.logo_image_id`·`logo_url`).
- 연결한 이미지(기업 로고 포함)는 `image_assets`에서 게시글 없이 `ATTACHED`로 두어 24시간 정리 대상에서 빠집니다. 바꾸거나 지우면 이전 이미지를 `UNREFERENCED`로 돌려 보존 기간(24시간) 뒤 S3에서 지웁니다.
- 내가 올린 임시 또는 참조 해제된 이미지만 연결할 수 있습니다. 남의 이미지, 게시글에 쓰는 이미지, 이미 정리된 이미지는 400 `IMAGE_ASSET_NOT_AVAILABLE`입니다. 반대로 프로필 이미지는 게시글에 연결할 수 없습니다.
- 같은 사용자가 동시에 두 번 바꾸면 먼저 끝난 쪽의 이미지가 정리되지 않고 남을 수 있습니다. 드문 경우라 잠금을 두지 않았습니다.

### 비밀번호 변경

```text
PATCH /api/v1/users/me/password   { "currentPassword": "...", "newPassword": "..." }
```

비밀번호를 가진 쪽에서 바꿉니다.

| 계정 | 처리 |
| --- | --- |
| 일반 회원 | 렛츠커리어 `PATCH /api/v1/internal/users/{userId}/password`로 전달한다. 렛츠커리어 로그인 비밀번호가 바뀐다 |
| 기업 회원 | 오공고가 BCrypt로 기존 비밀번호를 대조하고 새 값을 저장한다 |

렛츠커리어 호출은 트랜잭션 밖에서 합니다.

응답을 받지 못하면(연결 실패·시간 초과) 처음 요청을 포함해 같은 요청을 3번까지 보내고, 모두 실패하면 503 `LETSCAREER_UNAVAILABLE`입니다. 이 호출만 응답 대기를 3초로 둡니다(연결 2초). 최악의 경우 약 15초로, 오공고 앞단 API Gateway의 29초 제한 안에 들어오게 하기 위해서입니다. 응답이 온 실패(4xx·5xx)는 다시 보내도 결과가 같으므로 재시도하지 않습니다. 응답만 유실되고 앞선 요청이 이미 반영됐을 수 있으므로, 렛츠커리어는 새 비밀번호가 이미 저장된 값이면 기존 비밀번호를 대조하지 않고 성공으로 응답합니다. 재시도가 모두 응답 없이 끝난 경우에는 실제로 바뀌었는지 오공고가 알 수 없으며, 이메일 가입 회원은 렛츠커리어 비밀번호 찾기로 복구할 수 있습니다.

렛츠커리어의 오류 코드는 다음과 같이 옮깁니다.

| 렛츠커리어 | 오공고 |
| --- | --- |
| 400 `MISMATCH_PASSWORD` | 400 `CURRENT_PASSWORD_MISMATCH` |
| 400 `INVALID_PASSWORD` (8자 이상·특수문자 포함 위반) | 400 `INVALID_NEW_PASSWORD` |
| 400 `INVALID_AUTH_PROVIDER_*` (카카오·네이버·구글 가입) | 400 `SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE` |
| 그 밖의 4xx, 5xx, 통신 실패 | 503 `LETSCAREER_UNAVAILABLE` |

`GET /api/v1/users/me`의 `passwordChangeable`로 변경란을 보여줄지 판단합니다. 기업 회원과 이메일(`SERVICE`)로 가입한 일반 회원은 `true`, 카카오·네이버·구글 가입은 `false`입니다. 가입 경로를 아직 모르는 계정(렛츠커리어 값이 없는 과거 계정, 새 코드 배포 뒤 아직 로그인하지 않은 계정)은 `true`로 두고 렛츠커리어 판정에 맡깁니다.

요청 단계에서는 두 값이 비었는지와 새 비밀번호 길이(8~64자)만 봅니다. 특수문자 규칙은 일반 회원에게만 있으므로 렛츠커리어가 판정합니다. 이미 발급된 오공고 토큰은 비밀번호를 바꿔도 그대로 유효합니다.

### 두 계정이 공유하는 것

발급하는 `OG-access`·`OG-refresh`는 계정 종류와 무관하게 같습니다. 따라서 `POST /api/v1/auth/token`(재발급)과 `POST /api/v1/auth/signout`은 두 계정이 그대로 공유합니다. 로그인 가능 상태 검사(`ACTIVE`만 허용)도 `SignInValidator` 하나를 공유합니다.

### 역할을 토큰에 담지 않는 이유

`OG-access`는 사용자 식별자만 담고 역할은 담지 않습니다. 역할을 클레임에 넣으면 최대 액세스 토큰 수명(30분)만큼 낡은 역할이 남습니다. 역할이 필요한 엔드포인트는 Business Service가 `UserReader`로 현재 역할을 조회합니다.

그래서 클라이언트도 토큰을 열어 역할을 알 수 없고, `GET /api/v1/users/me`로 읽습니다. 기업 회원 화면을 열지 판단하는 기준은 이 응답의 `role`입니다.

```text
GET /api/v1/users/me
→ 200 {
    "userId": 17, "role": "USER", "status": "ACTIVE",
    "email": "...", "joinedAt": "...",
    "profile": { "name": "...", "nickname": "...", "profileImageUrl": "..." },
    "companyProfile": null
  }
```

`role`이 `COMPANY`이면 `companyProfile`이, 그 밖이면 `profile`이 채워지고 반대쪽은 `null`입니다. 계정 종류에 없는 프로필 테이블은 조회하지 않습니다.

**정지·탈퇴한 계정도 403이 아니라 200으로 응답하고 `status`에 현재 상태를 담습니다.** 다른 기업 회원 엔드포인트는 `verifyCompany`에서 403 `USER_SUSPENDED`·`USER_WITHDRAWN`으로 막지만, 이 경로는 자기 자신을 보는 조회이고 상태가 바뀐 뒤에도 액세스 토큰이 만료까지 유효하므로 클라이언트가 왜 다른 요청이 막히는지 알 수 있어야 합니다.

## 6. 렛츠커리어 내부 API

오공고가 부르는 렛츠커리어 내부 API는 다음과 같습니다.

| 경로 | 호출 시점 |
| --- | --- |
| `POST /api/v1/internal/auth/verify` | 매 로그인 |
| `GET /api/v1/internal/users/{userId}/job-profile` | 최초 가입 시 1회 |
| `PATCH /api/v1/internal/users/{userId}/password` | 일반 회원의 비밀번호 변경 |
| `PUT /api/v1/internal/users/{userId}/job-profile` | 오공고에서 고친 학력·희망 조건 전송(아웃박스) |
| `GET /api/v1/internal/challenges/recommend?userId=` | 추천 챌린지 조회마다. `userId`는 선택 |

반대로 렛츠커리어가 부르는 오공고 사용자 API의 내부 경로는 `PUT /api/v1/internal/letscareer-users/{letsCareerUserId}/job-profile` 하나입니다(렛츠커리어에서 고친 학력·희망 조건). 같은 내부 API 키로 인증하며 사용자 토큰으로는 부를 수 없습니다.

모두 서버 간 호출 전용이며 브라우저에 노출하지 않습니다. `X-Internal-Api-Key` 헤더가 서버 설정값과 일치할 때만 `INTERNAL` 권한을 부여하고, 그 외에는 인가 단계에서 차단합니다. 키가 설정되지 않으면 모든 요청을 거부합니다.

응답에는 연동에 필요한 최소 정보만 담습니다. 연락처는 마이페이지에 보여줄 휴대폰 번호만, 계정 정보는 가입 경로만 `verify`에 더 담고, 결제와 지원 이력은 포함하지 않습니다.

## 7. 설정

| 서버 | 키 | 설명 |
| --- | --- | --- |
| 렛츠커리어 | `spring.security.internal.api-key` | 내부 API 키. 미설정 시 내부 API 전면 차단 |
| 오공고 관리자 | `ogonggo.admin.internal.api-key` | 크롤러 등 내부 클라이언트용 키. 미설정 시 내부 API 전면 차단 |
| 오공고 | `ogonggo.auth.jwt.secret` | Base64 HS512 시크릿. 렛츠커리어와 공유하지 않는다 |
| 오공고 | `ogonggo.auth.jwt.access-token-validity` | 기본 30분 |
| 오공고 | `ogonggo.auth.jwt.refresh-token-validity` | 기본 14일 |
| 오공고 | `ogonggo.letscareer.base-url` | 렛츠커리어 서버 주소 |
| 오공고 | `ogonggo.letscareer.internal-api-key` | 렛츠커리어와 같은 값 |
| 오공고 | `spring.data.redis.host`, `spring.data.redis.port` | 리프레시 토큰 저장소 |

두 서버의 내부 API 키는 같은 값이어야 하며, JWT 시크릿은 반드시 서로 달라야 합니다.

## 7-1. 내부 클라이언트 인증

크롤러처럼 사람이 아닌 클라이언트는 사용자 로그인 흐름을 쓰지 않고 관리자 API의 `/api/v1/internal/**`만 사용합니다. 인증은 `X-Internal-Api-Key` 헤더 하나로 하며, 값이 `ogonggo.admin.internal.api-key`와 같을 때만 `ROLE_INTERNAL` 권한을 부여합니다.

키를 비교할 때는 비교에 걸린 시간으로 키를 추측할 수 없도록 상수 시간 비교를 사용합니다. 키를 설정하지 않으면 어떤 요청도 통과하지 못하므로 내부 API가 전면 차단됩니다. 키가 없거나 다르면 401 `UNAUTHORIZED`로 응답합니다.

이 키는 렛츠커리어와 주고받는 `ogonggo.letscareer.internal-api-key`와 **다른 값이어야 합니다.** 두 키는 방향과 상대가 다릅니다.

## 7-2. 브라우저 CORS 허용 오리진

> 오리진 목록은 배포 설정이 아니라 코드로 관리한다.

프론트엔드는 API와 다른 오리진에서 동작하므로 브라우저가 CORS를 강제합니다. 두 API 모듈의 SecurityFilterChain이 모두 `anyRequest().denyAll()`로 끝나기 때문에, 설정이 없으면 preflight가 인가 단계에서 막혀 모든 브라우저 호출이 실패합니다.

허용 오리진은 사용자 API와 관리자 API가 각각 소유합니다. 두 모듈은 독립 배포 경계이고 화면 도메인이 서로 다르게 바뀌므로, 공통 클래스로 묶지 않고 각 모듈의 `config` 패키지에 둡니다.

| 모듈 | 위치 |
| --- | --- |
| 사용자 API | `UserSecurityConfiguration.ALLOWED_ORIGIN_PATTERNS` |
| 관리자 API | `AdminSecurityConfiguration.ALLOWED_ORIGIN_PATTERNS` |

두 API에는 각자의 Swagger UI를 띄우는 API Gateway 주소가 하나씩 더 있고, 나머지는 두 목록이 같습니다.

| 오리진 | 모듈 | 용도 |
| --- | --- | --- |
| `https://www.ogonggo.co.kr` | 공통 | 운영 프론트엔드 |
| `https://ogonggo.co.kr` | 공통 | apex 도메인 직접 접속 |
| `http://localhost:[*]` | 공통 | 로컬 개발 서버. 포트는 사람과 프레임워크마다 달라 전부 연다 |
| `https://qi9peez04m.execute-api.ap-northeast-2.amazonaws.com` | 사용자 API | API Gateway 주소. 여기서 Swagger UI를 띄워 API를 호출한다 |
| `https://p5itn5nbm4.execute-api.ap-northeast-2.amazonaws.com` | 관리자 API | API Gateway 주소. 여기서 Swagger UI를 띄워 API를 호출한다 |

공통 설정은 `allowedMethods = *`, `allowedHeaders = *`, `allowCredentials = true`, `maxAge = 3600`입니다.

오리진은 `allowedOrigins`가 아니라 **`allowedOriginPatterns`**로 등록합니다. `allowedOrigins`는 와일드카드를 받지 않아 포트를 열 수 없고, `*` 하나만 넣는 형태는 `allowCredentials = true`와 함께 쓸 수 없습니다. `allowedOriginPatterns`는 요청 오리진을 그대로 되돌려주므로 자격증명을 켠 채로 패턴을 쓸 수 있습니다. 포트만 열렸을 뿐 호스트는 그대로라 `http://localhost.evil.com:3000` 같은 오리진은 차단되며, 이 성질도 테스트로 고정합니다.

**오리진을 코드에 두는 이유**는 `application.yml`이 배포 시 GitHub Secret(`APPLICATION_SECRET_USER`, `APPLICATION_SECRET_ADMIN`)으로 통째 덮어써지기 때문입니다. 오리진을 프로퍼티로 빼면 Secret에 키를 함께 넣어야 하고, 누락되면 운영에서만 CORS가 깨집니다. 코드에 두면 변경이 리뷰와 이력에 남습니다. 오리진을 추가하려면 해당 모듈의 `ALLOWED_ORIGIN_PATTERNS`를 고치고 배포합니다.

**`allowCredentials = true`인 이유**는 현재 인증이 `Authorization` 헤더 기반이라 필요하지 않지만, 쿠키 방식으로 바뀌어도 설정이 깨지지 않게 하기 위해서입니다. 오리진을 와일드카드 없이 전부 명시하므로 임의의 사이트가 자격증명을 실어 보낼 수 없습니다. 이 성질은 두 모듈의 `*CorsConfigurationTest`가 목록 밖 오리진 preflight를 403으로 고정해 지킵니다.

**Swagger UI가 API와 같은 오리진에 있어도 CORS 대상입니다.** 브라우저는 GET·HEAD가 아닌 요청에는 동일 오리진에도 `Origin` 헤더를 붙이고, 스프링은 `Origin`이 있으면 CORS 요청으로 처리합니다(Spring 5.0에서 동일 오리진 예외가 빠졌습니다). 게다가 앱이 API Gateway 뒤에 있어 자기 공개 주소를 알지 못해 스스로와 비교할 수도 없습니다. 그래서 Swagger를 띄우는 주소도 목록에 넣어야 하며, 넣지 않으면 조회(GET)는 되는데 `POST`만 `403 Invalid CORS request`로 막혀 원인을 찾기 어렵습니다.

관리자 API의 내부 경로(`X-Internal-Api-Key`)는 크롤러의 서버 간 호출이라 CORS와 무관합니다. CORS는 브라우저만 강제하는 규칙이라 서버 간 호출에는 영향이 없습니다. 같은 이유로 `curl`로는 오리진과 무관하게 호출됩니다.

**확인 필요**: 운영 프론트엔드는 HTTPS인데 API는 아직 ALB의 HTTP 주소(`http://ogonggo-alb-....elb.amazonaws.com`)뿐입니다. HTTPS 페이지에서 HTTP API를 호출하면 CORS 이전에 mixed content로 차단됩니다. ACM 인증서와 ALB 443 리스너, API 서브도메인이 준비되어야 운영에서 실제로 호출할 수 있습니다.

## 7-3. 관리자 콘솔 인증

> 관리자도 사용자 API의 로그인으로 `OG-access`를 받는다. 관리자 API는 토큰을 발급하지 않고 검증만 한다.

관리자 콘솔 경로(`/api/v1/admin/**`)는 `AdminAuthenticationFilter`가 `Authorization: Bearer`의 서명·만료·`type=access`를 확인하고, `AdminAuthService`가 `UserReader`로 현재 역할과 상태를 읽어 `ADMIN`이면서 `ACTIVE`일 때만 `ROLE_ADMIN` 권한을 줍니다. 크롤러의 `/api/v1/internal/**`은 이 필터를 거치지 않습니다.

역할은 토큰에 없으므로 요청마다 조회합니다. 역할을 회수하거나 계정을 정지하면 액세스 토큰이 만료되기 전이라도 다음 요청부터 막힙니다. 콘솔 안에서 권한을 더 나누지 않습니다.

| 상황 | 응답 |
| --- | --- |
| 토큰 없음, 서명·만료·종류 불일치 | 401 `UNAUTHORIZED` |
| 유효한 토큰이지만 활성 관리자가 아님 | 403 `FORBIDDEN` |

서명 시크릿은 사용자 API와 같은 `ogonggo.auth.jwt.secret`을 씁니다. 관리자 배포 설정(`APPLICATION_SECRET_ADMIN`)에도 같은 값을 넣어야 하며, 비어 있으면 모든 토큰 검증이 실패해 관리자 콘솔 API가 전면 401이 됩니다.

`ADMIN` 역할은 콘솔이나 API로 부여하지 않고 운영자가 DB에서 직접 바꿉니다(`users.role = 'ADMIN'`). 부여 이력과 감사 기록은 미정입니다.

콘솔 운영 도메인 `https://admin.ogonggo.co.kr`은 관리자 API의 `ALLOWED_ORIGIN_PATTERNS`에만 둡니다.

## 8. 아직 정하지 않은 것

| 주제 | 상태 | 현재 동작 |
| --- | --- | --- |
| 렛츠커리어 탈퇴·정지의 오공고 전파 | 미정 | 전파하지 않는다. 최대 `OG-refresh` 수명만큼 오공고 세션이 남는다 |
| 기업 회원 이메일 인증 | 미정 | 인증하지 않는다. 아무 이메일로나 가입할 수 있다 |
| 기업 회원 비밀번호 재설정 | 미정 | 재설정 경로가 없다 |
| 기업 회원 승인 | 없음으로 결정 | 가입 즉시 기업 기능을 사용한다 |
| 오공고 단독 탈퇴 | 미정 | 탈퇴 API가 없다. `User.withdraw`는 호출되지 않는다 |
| 탈퇴 사용자의 재로그인 | 확인 필요 | 403으로 막는다. 현재 도메인은 탈퇴를 되돌릴 수 없다고 선언하고 있다 |
| 렛츠커리어 로그아웃 시 오공고 동시 로그아웃 | 미정 | 오공고 세션은 유지된다 |
| `ADMIN` 역할 부여 경로 | DB 직접 부여로 결정(2026-09-14) | 부여 API와 화면은 없다. 부여 이력·감사 기록은 미정이다. 렛츠커리어의 `isAdmin`은 반영하지 않는다 |
| 프로필 수정 | 일부 결정(2026-09-29) | 구직 프로필과 수신 이메일은 오공고에서 고친다. 렛츠커리어에서 복제한 이름·가입 이메일·휴대폰 번호·닉네임은 조회만 하고 로그인 시 갱신된다. 프로필 이미지는 오공고 전용 이미지로 바꿀 수 있다(2026-09-30). 기업 정보는 `PUT /api/v1/users/me/company-profile/basic-info`·`manager-info`로 고친다 |

앞의 세 가지는 서로 얽혀 있으므로 함께 결정합니다. 재발급 시점에 렛츠커리어를 재검증하는 방식(`last_synced_at`이 일정 기간을 넘겼을 때만 호출)이 전파 지연을 좁히는 후보이며, 탈퇴 정책이 정해진 뒤 함께 검토합니다.

## 9. 검토한 대안

| 대안 | 제외 이유 |
| --- | --- |
| JWT 시크릿 공유 후 오공고가 렛츠커리어 토큰을 직접 검증 | 오공고가 렛츠커리어 토큰을 위조할 수 있고, 유출·교체 위험이 두 서비스에 함께 걸린다 |
| 렛츠커리어를 OAuth2/OIDC Authorization Server로 구축 | 1st-party 서비스 두 개에는 구축·운영 비용이 크다. 외부 클라이언트가 생기면 재검토한다 |
| 매 요청마다 렛츠커리어에 토큰 검증 요청 | 조회 경로가 외부 서버 가용성과 지연에 묶인다 |
| 프로필을 복제하지 않고 필요할 때마다 조회 | 같은 이유이며, `user_profiles` 스키마가 이미 복제를 전제하고 있다 |
| 리프레시 토큰을 MySQL 테이블에 보관 | Redis 도입이 확정되어 만료 처리를 저장소에 맡길 수 있다 |
| 최초 로그인에만 201을 반환 | 같은 엔드포인트가 상태 코드로 갈라지면 클라이언트 분기가 늘어난다. `isNewUser`로 구분한다 |
| 액세스 토큰 블랙리스트로 즉시 로그아웃 | 매 요청 Redis 조회가 필요하고, 짧은 액세스 토큰 수명으로 대체할 수 있다 |

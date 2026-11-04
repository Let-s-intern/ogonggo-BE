# 이번 알림 변경 FE 전달 문서

기준: `origin/main...feat/notification-member-topics`(`1bf566b`, `40f03db`). 이 문서는 **이번 브랜치에서 추가되거나 계약·동작이 달라진 API만** 다룬다. 관리자 API는 관리자 Bearer 토큰, 사용자 API는 로그인 사용자 Bearer 토큰이 필요하다.

## 변경 요약

| API | 이번 변경 | FE 영향 |
| --- | --- | --- |
| `GET /api/v1/admin/notification-batches/job-role-options` | **신규** | 희망 직군·직무 선택지 조회 |
| `POST /api/v1/images?usage=NOTIFICATION` | 기존 API·enum 입력 확장 | 관리자 화면에서 푸시 이미지 업로드 후 `imageId` 획득 |
| `POST /api/v1/admin/notification-batches` | 기존 API 확장 | 회원 그룹·희망 직무·광고성·이미지 발송 |
| `GET /api/v1/admin/notification-batches/{batchId}` | 기존 응답 확장 | 선택 직무·광고성·이미지 확인 |
| `GET /api/v1/admin/notification-batches/statistics` | **신규** | 기간별 FCM 접수·알림함 읽음 집계 |
| `GET /api/v1/users/me/notifications` | 기존 동작 변경 | 회원 역할에 맞는 토픽 공지만 목록에 노출. 응답 형식 동일 |
| `GET /api/v1/users/me/notifications/unread-count` | 기존 동작 변경 | 회원 역할에 맞는 공지만 미읽음에 포함. 응답 형식 동일 |
| `PATCH /api/v1/users/me/notifications/broadcasts/{batchId}/read` | 기존 동작 변경 | 현재 역할에 보이는 공지만 읽음 가능. 요청·응답 형식 동일 |
| `PATCH /api/v1/users/me/notifications/read-all` | 기존 동작 변경 | 현재 역할에 보이는 공지만 일괄 읽음. 요청·응답 형식 동일 |
| `PUT/DELETE /api/v1/users/me/fcm-token` | 기존 내부 동작 변경 | 토큰 등록·교체·삭제 시 회원 토픽 구독·해제 예약. 요청·응답 형식 동일 |
| `GET /api/v1/admin/enums`, `GET /api/v1/enums` | 기존 응답 값 추가 | 대상 3종·이미지 용도 1종 추가 |
| `POST /api/v1/images/presigned-urls` | 기존 API·enum 입력 확장 | 필요하면 `usage=NOTIFICATION`의 S3 직접 업로드 URL 발급 |

**발송 화면 흐름:** 대상/직무 선택 → (선택) 이미지 업로드 → 배치 생성 → `batchId`로 결과 조회 → 기간 통계 조회. 배치 생성 201은 **DB 적재 완료**이며 FCM 전달 완료가 아니다. 요청마다 새 배치를 만들므로 같은 요청을 반복하면 중복 발송될 수 있다.

API 분리 검토: 이미지 업로드는 기존 공용 API의 `usage=NOTIFICATION`으로 합쳤다. 희망 직무 선택지는 기존 `enums`의 `JobField`/`JobRole`과 다른 렛츠커리어 목록이며 `jobField`·`allRolesOption`·`jobRoles` 묶음이 필요해 별도 조회를 유지한다. 기간 통계는 단일 배치 결과와 달리 기간·대상별 집계이므로 별도 조회를 유지한다.

공통 응답은 성공 시 `{"status":200,"message":"요청이 성공했습니다.","data":...}`(생성은 201), 오류 시 `{"status":400,"code":"BAD_REQUEST","message":"..."}` 형태다. 아래 ID·시각·집계값은 형식 예시다. 시각은 Asia/Seoul 기준 offset 없는 ISO-8601이다.

---

## 1. 희망 직무 선택지 — 신규

```php
GET /api/v1/admin/notification-batches/job-role-options
```

요청 본문·쿼리 없음. `JOB_ROLES` 요청에 넣을 **문자열을 그대로** 받는다.

```json
{
  "status":200,"message":"요청이 성공했습니다.",
  "data":[{"jobField":"개발","allRolesOption":"개발 직무 전체","jobRoles":["백엔드/서버 개발","프론트엔드 개발"]}]
}
```

위 배열은 개발 직군의 직무를 일부만 발췌한 구조 예시다. 실제 전체 목록은 응답이 기준이다.

| 필드 | FE 사용 |
| --- | --- |
| `jobField` | 직군 이름 |
| `allRolesOption` | 해당 직군 전체를 선택할 때 `jobRoles[]`에 넣을 값 |
| `jobRoles[]` | 개별 직무를 선택할 때 넣을 값 |

| 오류 | 조건 |
| --- | --- |
| 401 `UNAUTHORIZED`, 403 `FORBIDDEN` | 관리자 인증·권한 오류 |

---

## 2. 푸시 이미지 업로드 — 기존 공용 API의 새 용도

```php
POST /api/v1/images?usage=NOTIFICATION
Content-Type: multipart/form-data
```

**사용자 API base URL**에 관리자 콘솔이 로그인 시 받은 오공고 액세스 토큰으로 호출한다. 관리자 API에는 별도 이미지 업로드 경로를 두지 않는다. 요청 파트는 `file` 하나이며 `usage=NOTIFICATION` 쿼리가 필수다. JPEG/PNG, 10 MiB 이하. 응답 `data.id`를 관리자 배치 생성의 `imageId`에 넣는다. 이미지를 다시 발송에 쓰려면 새로 업로드한다. 업로드 자체는 기존 공용 API이므로 로그인한 다른 회원도 호출할 수 있지만, 배치 연결은 관리자 권한과 이미지 소유자 검사로 제한된다.

```json
{
  "status":201,"message":"요청이 성공했습니다.",
  "data":{"id":"b7e8a0e4-7604-43f9-9b4e-cdc4702737f0","url":"https://cdn.example.com/notification/image.png","mimeType":"image/png","size":102400}
}
```

| 필드 | 의미 |
| --- | --- |
| `id` | 배치에 넣을 이미지 식별자 |
| `url` | 업로드된 이미지 주소; 배치에서는 `imageUrl`로 반환 |
| `mimeType`, `size` | 검증된 형식·바이트 크기 |

| 오류 | 조건 |
| --- | --- |
| 400 `IMAGE_FILE_REQUIRED` | 파일 파트 없음 또는 빈 파일 |
| 400 `IMAGE_FILE_TYPE_NOT_SUPPORTED` | JPEG/PNG 외 또는 실제 파일 형식 불일치 |
| 400 `IMAGE_FILE_TOO_LARGE` | 10 MiB 초과 |
| 401 `UNAUTHORIZED` | 사용자 API에서 유효한 오공고 액세스 토큰이 없음 |
| 500 `IMAGE_UPLOAD_FAILED` | 저장소 업로드 실패 |

---

## 3. 관리자 발송 요청 — 기존 API 확장

```php
POST /api/v1/admin/notification-batches
Content-Type: application/json
```

**이번에 추가된 대상 값:** `GENERAL_MEMBERS`(활성 `USER`·`MENTOR`·`ADMIN`), `COMPANY_MEMBERS`(활성 `COMPANY`), `JOB_ROLES`(희망 직무가 선택지와 겹치는 활성 일반 회원). 기존 `USER_IDS`·`ALL`도 계속 사용한다. `직무 무관` 회원은 `JOB_ROLES` 대상이 아니다.

```json
{
  "targetType":"JOB_ROLES",
  "title":"직무별 공지",
  "body":"알림 본문",
  "data":{"url":"https://www.ogonggo.co.kr/announcements/10"},
  "jobRoles":["백엔드/서버 개발"],
  "advertisement":false,
  "imageId":"b7e8a0e4-7604-43f9-9b4e-cdc4702737f0"
}
```

| 필드 | 이번 변경과 사용 규칙 |
| --- | --- |
| `targetType` | 새 값 3개 추가. 필수 |
| `userIds` | 기존 `USER_IDS`에서만 사용. 새 대상 값에서는 생략 |
| `jobRoles` | **신규**. `JOB_ROLES`에서만 필수, 선택지 API의 문자열 1~100개. 다른 대상에서는 생략. 배열 항목 `null` 불가 |
| `advertisement` | **신규**, 기본 `false`. `true`면 렛츠커리어 마케팅 수신 동의자만 **DIRECT** 발송. `COMPANY_MEMBERS`와 조합 불가 |
| `imageId` | **신규**, 생략 시 `null`. 본인 소유·업로드 완료·미사용 `NOTIFICATION` 이미지 ID |
| `title`·`body`·`data` | 기존 계약 유지. 제목 최대 100자, 본문 최대 1,000자, `data` 최대 20개·값은 문자열. `data.url`은 호스트가 있는 http(s) URL 필수 |

대상별 요청 조합:

| `targetType` | `userIds` | `jobRoles` | 광고성 |
| --- | --- | --- | --- |
| `USER_IDS` | 기존처럼 1~1,000개 양수 ID 필수 | 생략 | 가능 |
| `ALL` | 생략 | 생략 | 가능 |
| `GENERAL_MEMBERS` | 생략 | 생략 | 가능 |
| `COMPANY_MEMBERS` | 생략 | 생략 | **불가** |
| `JOB_ROLES` | 생략 | 1~100개 필수 | 가능 |

**발송 방식:** `USER_IDS`·`JOB_ROLES` 및 `advertisement=true`는 항상 `DIRECT`. `ALL`은 기존 전체 토픽 설정, `GENERAL_MEMBERS`·`COMPANY_MEMBERS`는 새 회원 토픽 설정에 따라 `TOPIC` 또는 `DIRECT`다. `DIRECT`는 회원별 알림함과 토큰별 발송 작업을 만들고, `TOPIC`은 공통 공지와 토픽 작업 1건을 만든다. 실제 방식은 응답의 `deliveryTargetType`으로 판단한다. 토큰 없는 회원도 직접 발송의 알림함에는 남는다. 일치 회원·동의자가 0명이면 0건 배치가 생성될 수 있다.

```json
{
  "status":201,"message":"요청이 성공했습니다.",
  "data":{
    "batchId":"f4d9e0ad-630c-4c15-9480-cad45690320e",
    "targetUserCount":3,"recipientUserCount":2,"enqueuedCount":3,
    "skippedNoTokenUserCount":1,"skippedInactiveUserCount":0,
    "deliveryTargetType":"DIRECT","noTokenUserCount":1,
    "advertisement":false,"noMarketingConsentUserCount":0,
    "imageId":"b7e8a0e4-7604-43f9-9b4e-cdc4702737f0",
    "imageUrl":"https://cdn.example.com/notification/image.png"
  }
}
```

| 응답 필드 | 의미 |
| --- | --- |
| `batchId` | 결과 조회에 사용 |
| `deliveryTargetType` | 기존 필드. 새 대상·광고성에도 적용되는 실제 `DIRECT`/`TOPIC` |
| `advertisement` | **신규**. 광고성 여부 |
| `noMarketingConsentUserCount` | **신규**. 광고성 발송에서 동의하지 않아 제외한 활성 회원 수 |
| `imageId`, `imageUrl` | **신규**. 이미지 없이 발송하면 둘 다 `null` |
| `targetUserCount`, `recipientUserCount` | 기존 필드. 대상 회원 수, 요청 시점 활성 토큰 보유 **회원 수** |
| `enqueuedCount` | 기존 필드. `DIRECT`는 토큰별 작업 수, `TOPIC`은 1. 기기 도착 수 아님 |
| `noTokenUserCount` | 기존 필드. 토큰 없는 활성 대상 회원 수; `skippedNoTokenUserCount`는 동일 값의 호환용 별칭 |
| `skippedInactiveUserCount` | 기존 필드. 비활성으로 제외한 회원 수 |

| 오류 | 이번 변경과 관련된 조건 |
| --- | --- |
| 400 `BAD_REQUEST` | `jobRoles`가 없거나 잘못된 값, 새 대상과 `userIds`/`jobRoles` 조합 오류, `COMPANY_MEMBERS`+광고성, 시스템 필드·이미지를 포함한 FCM payload 크기 초과(`DIRECT` 4,096/`TOPIC` 2,048바이트) |
| 400 `IMAGE_ASSET_NOT_AVAILABLE` | 다른 소유자·용도, 미완료·이미 사용한 이미지 등 |
| 503 `MARKETING_AGREEMENT_UNAVAILABLE` | 광고성 발송의 렛츠커리어 동의 확인 실패. 배치 생성 안 됨 |
| 401 `UNAUTHORIZED`, 403 `FORBIDDEN` | 관리자 인증·권한 오류 |

기존 `USER_IDS`에 없는 회원이 포함되면 404 `USER_NOT_FOUND`로 전체 요청을 거부한다. 201은 적재 완료이며 실제 FCM 처리 결과는 아래 결과 API에서 확인한다.

---

## 4. 배치 결과 — 기존 응답 확장

```php
GET /api/v1/admin/notification-batches/{batchId}
```

요청은 기존처럼 배치 UUID 경로 변수. 이번에는 아래 필드가 **추가**된다. 나머지 발송 상태 필드는 기존 계약과 같다.

```json
{
  "status":200,"message":"요청이 성공했습니다.",
  "data":{
    "batchId":"f4d9e0ad-630c-4c15-9480-cad45690320e",
    "requestedByUserId":7,"createdAt":"2026-11-03T10:00:00",
    "targetType":"JOB_ROLES","title":"직무별 공지","body":"알림 본문",
    "targetUserCount":3,"recipientUserCount":2,"enqueuedCount":3,
    "skippedNoTokenUserCount":1,"skippedInactiveUserCount":0,
    "sentCount":2,"failedCount":1,"unknownCount":0,"pendingCount":0,"expiredCount":0,
    "completed":true,"dispatchExpiresAt":"2026-11-04T10:00:00",
    "resultsAvailableUntil":"2026-12-03T10:00:00",
    "failureReasons":[{"status":"FAILED","resultCode":"UNREGISTERED","count":1}],
    "deliveryTargetType":"DIRECT","noTokenUserCount":1,
    "targetJobRoles":["백엔드/서버 개발"],
    "advertisement":false,"noMarketingConsentUserCount":0,
    "imageId":null,"imageUrl":null
  }
}
```

| 추가 필드 | 의미 |
| --- | --- |
| `targetJobRoles` | `JOB_ROLES`에 사용한 선택지 배열; 다른 대상은 `null` |
| `advertisement`, `noMarketingConsentUserCount` | 광고성 여부·동의하지 않아 제외한 활성 회원 수 |
| `imageId`, `imageUrl` | 사용 이미지; 없으면 각각 `null` |

기존 `sentCount`는 FCM **접수 작업 수**다. `DIRECT`는 기기별, `TOPIC`은 토픽 요청별이다. `completed=true`여도 `failedCount`·`unknownCount`·`expiredCount`를 확인해야 한다. 생성 후 30일이 지나면 기존처럼 404 `NOTIFICATION_BATCH_NOT_FOUND`다.

| 오류 | 조건 |
| --- | --- |
| 400 `BAD_REQUEST` | UUID 형식 오류 |
| 401 `UNAUTHORIZED`, 403 `FORBIDDEN` | 관리자 인증·권한 오류 |
| 404 `NOTIFICATION_BATCH_NOT_FOUND` | 없거나 조회 기간 만료 |

---

## 5. 기간별 발송·읽음 통계 — 신규

```php
GET /api/v1/admin/notification-batches/statistics?from=2026-11-01T00:00:00&to=2026-11-02T00:00:00&targetType=GENERAL_MEMBERS
```

| 쿼리 | 규칙 |
| --- | --- |
| `from`, `to` | 둘 다 선택·함께 지정. 둘 다 생략하면 현재까지 최근 30일. 시작 포함, 종료 제외; 시작은 최근 30일 이내, 범위 최대 30일, 종료는 현재 시각 다음 날 이하 |
| `targetType` | 선택. 생략하면 모든 대상 종류 |

```json
{
  "status":200,"message":"요청이 성공했습니다.",
  "data":{
    "from":"2026-11-01T00:00:00","to":"2026-11-02T00:00:00",
    "groups":[{
      "targetType":"GENERAL_MEMBERS","deliveryTargetType":"DIRECT",
      "batchCount":1,"targetUserCount":2,"recipientUserCount":1,"enqueuedCount":2,
      "sentCount":1,"failedCount":1,"unknownCount":0,"pendingCount":0,"expiredCount":0,
      "readUserCount":1,"readEligibleUserCount":2,"readRate":0.5
    }]
  }
}
```

| 응답 필드 | 의미 |
| --- | --- |
| `groups[]` | `targetType`+`deliveryTargetType`별 합계; 해당 배치가 없으면 `[]` |
| `sentCount` | FCM 접수 **작업** 수. 단말 도착·푸시 클릭 수 아님 |
| `readUserCount` | 배치별로 한 회원을 한 번 세고 합산한 알림함 읽음 회원 수 |
| `readEligibleUserCount` | `DIRECT`는 개인 알림함 행 수, `TOPIC`은 발송 시점 대상 회원 수 |
| `readRate` | `readUserCount / readEligibleUserCount`; 분모 0이면 `null` |

`TOPIC` 읽음 수는 **현재 활성 상태와 역할에 보이는 공지**를 읽은 회원 기준이다. 발송 뒤 역할이 변하면 발송 시점 분모와 현재 읽음 수의 모집단이 다를 수 있다. 생성 후 30일 지난 배치는 집계에서 제외된다.

| 오류 | 조건 |
| --- | --- |
| 400 `BAD_REQUEST` | 기간 형식·짝·순서·범위 오류, 잘못된 대상 값 |
| 401 `UNAUTHORIZED`, 403 `FORBIDDEN` | 관리자 인증·권한 오류 |

---

## 6. 사용자 알림함 — 기존 API의 대상 필터 변경

이번 브랜치는 **사용자 알림 API의 경로나 요청·응답 DTO를 바꾸지 않았다.** 공통 공지의 대상이 `ALL` 외에 `GENERAL_MEMBERS`·`COMPANY_MEMBERS`로 늘었고, 아래 기존 API들이 **현재 로그인 회원의 역할**을 기준으로 공지를 걸러 처리한다.

| 기존 API | 이번 동작 변경 |
| --- | --- |
| `GET /api/v1/users/me/notifications` | 최근 30일·가입 이후 공지 중 `ALL`과 본인 역할 그룹의 `TOPIC` 공지만 반환. 개인 알림은 기존 동작 |
| `GET /api/v1/users/me/notifications/unread-count` | 위 조건에 맞는 공통 공지만 미읽음 수에 포함 |
| `PATCH /api/v1/users/me/notifications/broadcasts/{batchId}/read` | 본인 역할에 보이지 않는 배치는 404 `USER_NOTIFICATION_NOT_FOUND`. 반복 읽음은 기존처럼 200 |
| `PATCH /api/v1/users/me/notifications/read-all` | 본인 역할에 보이는 공통 공지만 일괄 읽음; 개인 알림 처리는 기존 동작 |

기존 목록 응답의 `source=BROADCAST`, `batchId`, `sourceId`, `isRead` 형식은 그대로다. **개인 알림 한 건 읽음 API는 이번 브랜치에서 변경되지 않았다.** 기존처럼 공통 공지를 한 건 읽을 때는 `broadcasts/{batchId}/read`를 사용한다. 읽음 기록은 새 관리자 통계의 `readUserCount` 집계에 사용된다.

---

## 7. enum·공용 이미지·토큰의 간접 영향

### Enum 값 추가

```php
GET /api/v1/admin/enums
GET /api/v1/enums
```

기존 `data.NotificationBatchTargetType[]`에 `GENERAL_MEMBERS`·`COMPANY_MEMBERS`·`JOB_ROLES`, `data.ImageAssetUsage[]`에 `NOTIFICATION`(푸시 알림 이미지)이 추가된다. 항목 형식 `{"name":"GENERAL_MEMBERS","desc":"활성 일반 회원","parent":null}`은 그대로다. 기존 enum 표시 UI는 새 `name`을 처리하면 된다.

### Presigned 이미지 입력 확장

```php
POST /api/v1/images/presigned-urls
```

기존 공용 이미지 API의 `usage`에 `NOTIFICATION`을 사용할 수 있다. Presigned 요청 예: `{"usage":"NOTIFICATION","contentType":"image/png","size":102400}`. 이 용도는 JPEG/PNG·10 MiB 이하. 발급 URL에 파일을 올린 뒤 기존 `POST /api/v1/images/{imageId}/complete`를 호출해야 배치에 붙일 수 있다. **관리자 발송에 붙일 이미지는 업로드 소유자와 발송 관리자 ID가 같아야 한다.** 공용 API의 응답 구조는 변경되지 않았다.

### FCM 토큰 내부 동작

```php
PUT /api/v1/users/me/fcm-token
DELETE /api/v1/users/me/fcm-token
```

기존 요청·응답은 그대로다. 토큰 등록·교체 시 회원 역할·상태에 맞는 전체/일반/기업 토픽 구독 작업을 예약하고, 교체·삭제 시 기존 토큰의 구독 해제를 예약한다. 서버가 비동기로 FCM에 반영하므로 **토큰 API의 200은 토픽 구독 완료를 뜻하지 않는다.** 푸시 수신은 동기화 지연의 영향을 받을 수 있다.

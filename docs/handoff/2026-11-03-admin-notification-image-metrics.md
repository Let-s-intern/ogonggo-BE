# 관리자 푸시 이미지·통계 API 전달사항

기준: `feat/notification-member-topics`의 이미지·기간 통계 구현. 어드민 화면 연결용 계약이다.

## 이미지 업로드와 발송

1. 관리자 Bearer 토큰으로 `POST /api/v1/admin/notification-images`에 `multipart/form-data`의 `file`을 보낸다. JPEG·PNG, 최대 10 MiB다.
2. 201 응답의 `data.id`를 `POST /api/v1/admin/notification-batches` 요청의 선택 필드 `imageId`에 넣는다. 한 이미지는 한 배치에만 연결할 수 있으므로 재발송 때 새로 업로드한다.
3. 배치 생성 201 응답과 `GET /api/v1/admin/notification-batches/{batchId}` 200 응답에는 `imageId`, `imageUrl`이 있다. 이미지 없이 보낸 경우 둘 다 `null`이다. FCM 메시지의 이미지 필드로 전달하며 알림함 API에는 이미지 필드를 추가하지 않았다. 실제 단말 표시는 운영 환경에서 확인한다.

```json
{"targetType":"GENERAL_MEMBERS","title":"공지","body":"본문","data":{"url":"https://www.ogonggo.co.kr/announcements/10"},"imageId":"b7e8a0e4-7604-43f9-9b4e-cdc4702737f0"}
```

- 다른 관리자의 이미지, 업로드 미완료·이미 연결된 이미지, 알림 외 용도나 지원하지 않는 형식, HTTPS가 아닌 URL은 400 `IMAGE_ASSET_NOT_AVAILABLE`이다. 형식이 잘못된 ID나 없는 ID도 발송 기록 없이 거부한다.
- 배치 생성의 DIRECT/TOPIC 결정은 기존 설정과 광고성 발송 규칙을 따른다. 이미지 여부가 대상 선정에 영향을 주지 않는다.

## 기간별 통계

`GET /api/v1/admin/notification-batches/statistics?from=2026-11-01T00:00:00&to=2026-11-02T00:00:00&targetType=GENERAL_MEMBERS`

- `from` 이상, `to` 미만이다. 둘 다 생략하면 현재까지 최근 30일을 조회한다. 지정할 때는 둘 다 필요하다. `targetType`은 선택이며 기존 `NotificationBatchTargetType` 값이다.
- 30일보다 오래된 시작, 30일 초과 범위, 현재 시각 다음 날을 넘는 종료는 400 `BAD_REQUEST`다. 배치 생성 후 30일이 되는 시각부터 결과를 통계에서도 제외한다.
- `data.groups`는 `targetType`과 `deliveryTargetType`(DIRECT/TOPIC)별 목록이다. 데이터가 없으면 `[]`다.

```json
{"data":{"from":"2026-11-01T00:00:00","to":"2026-11-02T00:00:00","groups":[{"targetType":"GENERAL_MEMBERS","deliveryTargetType":"DIRECT","batchCount":1,"targetUserCount":2,"recipientUserCount":1,"enqueuedCount":2,"sentCount":1,"failedCount":1,"unknownCount":0,"pendingCount":0,"expiredCount":0,"readUserCount":1,"readEligibleUserCount":2,"readRate":0.5}]}}
```

- `sentCount`는 Firebase 접수 작업 수다. DIRECT는 기기 수, TOPIC은 토픽 요청 수다. 단말 도착 수나 푸시를 누른 횟수가 아니다.
- `readUserCount`는 배치별로 한 회원을 한 번만 센 뒤 합친 값이다. 같은 회원이 여러 배치를 읽으면 각각 센다. DIRECT는 개인 알림함, TOPIC은 현재 활성 상태와 대상 역할에 맞는 공통 알림 읽음을 센다.
- `readEligibleUserCount`는 DIRECT 개인 알림함 수와 TOPIC 발송 시점 대상 회원 수다. `readRate`는 `readUserCount / readEligibleUserCount`이며 분모가 0이면 `null`이다. TOPIC에서 발송 후 역할·회원 수가 바뀌면 읽음률은 발송 시점의 대상 규모와 현재 읽음 기록을 비교한 참고 값이다.
- 기존 배치별 성공·실패 상세는 `GET /api/v1/admin/notification-batches/{batchId}`를 사용한다. 통계 API는 원본 토큰이나 회원별 읽음 목록을 제공하지 않는다.

## 공용 이미지 용도 추가에 따른 간접 영향

- `GET /api/v1/admin/enums`, `GET /api/v1/enums`: `data.ImageAssetUsage` 목록에 `NOTIFICATION`(푸시 알림 이미지)이 추가된다.
- `POST /api/v1/images`의 선택 쿼리 `usage`와 `POST /api/v1/images/presigned-urls`의 요청 `usage`도 공용 enum을 사용하므로 `NOTIFICATION`을 받는다. 이 사용자 API에서 올린 이미지는 소유자가 같을 때만 관리자 발송에 연결할 수 있다.
- `POST /api/v1/images/{imageId}/complete`는 새 용도의 presigned 업로드도 기존 방식으로 완료한다. 경로·요청·응답 형식은 바뀌지 않았다.
- 사용자 알림함 조회·읽음 API는 이번 변경에서 요청·응답 형식이 바뀌지 않았다. 읽음 기록은 새 관리자 통계의 집계 원천으로 사용한다.

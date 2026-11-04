# 알림 그룹·이미지·관리자 통계 로컬 E2E 결과 (2026-11-03)

대상: `feat/notification-member-topics` 작업 트리. 관리자 API(18081: 회원 토픽 게이트 꺼짐, 18082: 회원 토픽 게이트 켜짐), 사용자 API(18080), 임시 MySQL 8.0·Redis 7을 각각 격리해 실행했다. 기존 `go-mysql`과 기존 데이터는 사용하지 않았다. 모든 요청은 실제 `curl`로 보냈고 응답 본문과 DB 스냅샷은 [결과 JSON](notification-e2e-2026-11-03-results.json)에 수집했다.

후속 변경(2026-11-04): 아래 요청은 당시 실행 기록으로 보존한다. 관리자 이미지 전용 경로는 제거됐으며 현재는 사용자 API의 `POST /api/v1/images?usage=NOTIFICATION`을 사용한다. 새 경로의 계약 테스트는 별도로 통과했지만 아래 E2E 결과에 포함되지는 않는다.

## 실행한 핵심 요청

```bash
# ADMIN_JWT, USER_JWT는 격리 환경에서 만든 테스트용 JWT다.
curl -sS -H "Authorization: Bearer $ADMIN_JWT" -H 'Content-Type: application/json' \
  --data '{"targetType":"USER_IDS","userIds":[1002],"title":"E2E 공지","body":"이미지 직접 발송","data":{"url":"https://www.ogonggo.co.kr/e2e"},"imageId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"}' \
  http://127.0.0.1:18081/api/v1/admin/notification-batches
curl -sS -H "Authorization: Bearer $ADMIN_JWT" \
  'http://127.0.0.1:18081/api/v1/admin/notification-batches/statistics?from=2026-11-03T00%3A00%3A00&to=2026-11-04T00%3A00%3A00'
curl -sS -X PATCH -H "Authorization: Bearer $USER_JWT" \
  http://127.0.0.1:18080/api/v1/users/me/notifications/broadcasts/7e076ce8-9e4c-4c0d-b87b-eed9cdb9cad8/read
curl -sS -H "Authorization: Bearer $ADMIN_JWT" -F 'file=@push.png;type=image/png' \
  http://127.0.0.1:18081/api/v1/admin/notification-images
```

## 발송·알림함

| 요청 | HTTP | 대상 회원 | 푸시 수신 회원 | 적재 작업 | 방식 |
| --- | ---: | ---: | ---: | ---: | --- |
| 지정 회원, 이미지 없음 | 201 | 1 | 1 | 2 | DIRECT |
| 지정 회원, 이미지 있음 | 201 | 1 | 1 | 2 | DIRECT |
| 전체 회원, 이미지 있음 | 201 | 3 | 1 | 1 | TOPIC |
| 희망 직무(백엔드/서버 개발), 정상 픽스처 | 201 | 1 | 1 | 2 | DIRECT |
| 일반회원, 게이트 꺼짐 | 201 | 2 | 1 | 2 | DIRECT |
| 기업회원, 게이트 꺼짐 | 201 | 1 | 1 | 1 | DIRECT |
| 일반회원, 게이트 켜짐 | 201 | 2 | 1 | 1 | TOPIC |
| 기업회원, 게이트 켜짐 | 201 | 1 | 1 | 1 | TOPIC |

일반회원 대상 2명은 픽스처의 `USER`와 `ADMIN`이다. `UserRole.GENERAL_MEMBER_ROLES`에 `ADMIN`이 포함된다. 희망 직무의 첫 2회 요청은 임시 SQL 한글 인코딩 오류 때문에 각각 0명·0건으로 정상 기록됐다. DB 바이트를 UTF-8로 수정한 뒤 같은 요청이 1명·2건으로 적재됐다.

이미지 ID가 없는 발송의 이미지 필드는 `null`; 이미지가 있는 두 발송은 DB의 `image_assets`가 `ATTACHED`로 바뀌고 배치·FCM payload에 각 URL이 기록됐다. 잘못된 용도의 이미지 자산은 `TEMPORARY` 그대로 남았다. 일반회원 알림함에는 전체·일반회원 토픽만, 기업회원 알림함에는 전체·기업회원 토픽만 나타났다. 지정 회원 알림 1건과 전체 토픽 1건을 읽은 뒤 알림함 3건 중 2건이 읽음, 미읽음 수는 1이었다.

## 관리자 통계

초기 3개 배치의 읽음 전→후: `USER_IDS/DIRECT`는 `0/2→1/2`(50%), `ALL/TOPIC`은 `0/3→1/3`(33.3%). 마지막에 일반·기업 토픽도 각 1명씩 읽은 결과는 다음과 같다.

| 대상/방식 | 배치 | 대상 | 적재 | 읽음/대상 |
| --- | ---: | ---: | ---: | ---: |
| ALL/TOPIC | 1 | 3 | 1 | 1/3 |
| COMPANY_MEMBERS/DIRECT | 1 | 1 | 1 | 0/1 |
| COMPANY_MEMBERS/TOPIC | 1 | 1 | 1 | 1/1 |
| GENERAL_MEMBERS/DIRECT | 1 | 2 | 2 | 0/2 |
| GENERAL_MEMBERS/TOPIC | 1 | 2 | 1 | 1/2 |
| JOB_ROLES/DIRECT | 3 | 1 | 2 | 0/1 |
| USER_IDS/DIRECT | 2 | 2 | 4 | 1/2 |

`JOB_ROLES` 배치 3건에는 인코딩이 잘못된 픽스처로 만든 0명 배치 2건이 포함된다. 최종 DB 작업은 `DIRECT/PENDING=9`, `TOPIC/PENDING=3`으로 통계의 적재·대기 합계 12건과 일치한다. `targetType=USER_IDS` 필터는 해당 그룹만 반환했고, 빈 기간은 빈 배열, 기간 한쪽 누락과 30일 초과는 400, 미인증은 401이었다.

## 거부·업로드 경계와 수정

미인증 배치 발송 401, 일반회원의 관리자 API 접근 403, 존재하지 않거나 용도가 다른 이미지 400 `IMAGE_ASSET_NOT_AVAILABLE`, 기업회원 광고성 요청 및 없는 희망 직무 선택 400 `BAD_REQUEST`를 확인했다. 업로드의 잘못된 파일 형식은 400 `IMAGE_FILE_TYPE_NOT_SUPPORTED`였다.

E2E에서 관리자 업로드의 2 MiB 파일과 파일 누락 요청이 모두 500 `INTERNAL_SERVER_ERROR`인 계약 불일치를 발견했다. 관리자 multipart 한도를 10 MiB/요청 11 MiB로 설정하고 파일 누락·용량 초과 예외를 매핑했다. 수정 후 파일 누락은 400 `IMAGE_FILE_REQUIRED`, 11 MiB는 400 `IMAGE_FILE_TOO_LARGE`다. 2 MiB는 검증을 통과해 저장 단계까지 도달했고, 이 환경에는 S3가 설정되지 않아 500 `IMAGE_UPLOAD_FAILED`를 반환했다. 회귀 테스트는 수정 전 컴파일 실패, 수정 후 통과했다. 관리자 전체 테스트는 **298개 통과, 실패 0개**다.

## 검증 범위

별도 에이전트 세션에 코드·기대 결과의 독립 검증을 요청했으나 사용량 제한으로 실행되지 않았다. 대신 별도 Python 프로세스가 저장된 HTTP 응답과 DB 스냅샷을 **87개 단언**으로 대조해 통과했고, 그 결과를 다시 `AdminNotificationBatchService`, `NotificationBatchStatisticsReader`·쿼리, `UserNotificationService`, 이미지 검증·예외 처리 코드와 대조했다. 검증 스크립트의 직접 발송 대기 합계 초안(11건)은 DB가 제시한 9건을 따라 배치별 적재 합계를 다시 계산해 고쳤다.

S3 실업로드와 Firebase 실제 전송·토픽 구독은 외부 연동 정보가 없는 로컬 격리 환경에서 실행하지 않았다. 이미지 연결 검증에는 격리 DB에 직접 준비한 임시 이미지 자산을 사용했다. 따라서 `PENDING`은 발송 성공 수가 아니라 적재된 작업 수다.

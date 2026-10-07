-- 알림 실패 결과에 운영용 의미 분류를 추가한다.
-- 2026-10-06-notifications.sql 적용 후 실행한다. 기존 결과 코드는 원본값 보존을 위해 변경하지 않는다.
-- 운영은 ddl-auto=none이므로 사용자 API 배포 전에 한 번 적용한다.

alter table notifications
    add column result_category varchar(50) null after result_code;

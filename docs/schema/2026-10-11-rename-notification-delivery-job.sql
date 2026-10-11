-- 공용 알림 발송 스케줄러 이름 변경.
-- 기존 알림톡 전용 이름을 모든 채널을 포괄하는 이름으로 변경한다.
-- 운영 DB에 기존 초기 스키마를 적용한 경우 사용자 API 배포 전에 한 번 실행한다.
-- 초기 스키마를 아직 적용하지 않았다면 2026-10-06-notifications.sql의 최신 이름으로 생성되므로 이 SQL은 실행 결과가 없다.
update scheduled_jobs
set name = 'notificationDelivery',
    updated_at = now(6)
where name = 'jobBookmarkAlimTalkDelivery';

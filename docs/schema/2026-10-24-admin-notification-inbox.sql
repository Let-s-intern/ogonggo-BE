-- MySQL 8. 기존 사용자 알림함·관리자 발송 묶음 스키마 적용 후 한 번만 실행한다.
-- 사용자 API를 먼저 배포하고 관리자 API를 배포한다. 애플리케이션이 자동 실행하지 않는다.
-- 이전 관리자 발송의 알림함은 소급 생성하지 않는다. 운영 DB에서 실행한 기록이 아니다.
alter table user_notifications
    modify column post_id bigint null,
    modify column comment_id bigint null,
    add column batch_id varchar(36) null,
    add column url text null,
    add constraint uk_user_notification_batch_recipient unique (batch_id, recipient_user_id);

-- 댓글 알림의 batch_id·url은 null이며 기존 댓글 중복 방지 제약을 유지한다.
-- 관리자 알림은 ADMIN_NOTIFICATION으로 저장하고 회원별 알림함을 batch_id로 묶는다.
-- 기기별 발송은 기존 notifications.user_notification_id로 알림함을 참조한다.
-- 롤백 시 컬럼·행을 유지하고, 새 유형을 읽지 못하는 구버전 사용자 API로 되돌리지 않는다.

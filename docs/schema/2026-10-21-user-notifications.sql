-- MySQL 8. 다중 기기 토큰 이관(2026-10-21-user-fcm-tokens.sql) 이후 한 번만 적용한다.
-- 새 사용자 API 배포 전에 실행한다. 애플리케이션이 자동 적용하지 않는다.
-- 기존 댓글·알림을 소급 생성하지 않는다. 운영 DB에서 실행한 기록이 아니다.

create table user_notifications (
    id bigint not null auto_increment,
    recipient_user_id bigint not null,
    type varchar(40) not null,
    actor_user_id bigint not null,
    post_id bigint not null,
    comment_id bigint not null,
    actor_nickname varchar(100) not null,
    actor_profile_image_url varchar(2048) null,
    post_title varchar(255) not null,
    preview varchar(100) not null,
    push_skip_reason varchar(30) null,
    read_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,
    primary key (id),
    constraint uk_user_notification_event unique (type, comment_id, recipient_user_id),
    index idx_user_notification_recipient_created_id (recipient_user_id, created_at, id)
) engine = InnoDB default character set utf8mb4;

-- 기존 발송 행은 null 유지. 발송 기록은 독립적으로 정리되므로 FK/cascade는 두지 않는다.
alter table notifications
    add column user_notification_id bigint null,
    add index idx_notification_user_notification (user_notification_id);

-- 적재 후 운영 확인 예시. 값은 클라이언트에서 바인딩하고 토큰·payload·댓글 원문은 조회하지 않는다.
-- select id, recipient_user_id, type, post_id, comment_id, push_skip_reason, read_at, created_at
-- from user_notifications where id = :user_notification_id;
-- select id, user_notification_id, status, result_code, result_category, result_detail,
--        provider_message_id, sent_at, created_at
-- from notifications where user_notification_id = :user_notification_id order by id;
-- select id, user_notification_id, status, result_code, result_category, result_detail, updated_at
-- from notifications where channel = 'FCM' and status in ('FAILED', 'UNKNOWN') order by id desc limit 100;

-- 롤백 시 스키마와 저장된 행을 유지한다. DDL은 MySQL에서 자동 커밋된다.
-- 배포·롤백 절차: docs/architecture/notification-extension-guide.md

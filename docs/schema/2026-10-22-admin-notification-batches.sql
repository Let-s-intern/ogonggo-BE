-- MySQL 8. 2026-10-21의 다중 기기·알림함 스키마 적용 이후 한 번만 실행한다.
-- 두 API 배포 전에 적용하고 사용자 API를 먼저 배포한다. 운영 DB에 실행한 기록이 아니다.
-- DDL은 자동 커밋된다. 롤백할 때 새 컬럼·운영 기록을 삭제하지 않는다.

create table notification_batches (
    id varchar(36) not null,
    requested_by_user_id bigint not null,
    target_type varchar(20) not null,
    requested_user_ids_json text null,
    title varchar(100) not null,
    body varchar(1000) not null,
    data_json text not null,
    target_user_count bigint not null,
    recipient_user_count bigint not null,
    enqueued_count bigint not null,
    skipped_no_token_user_count bigint not null,
    skipped_inactive_user_count bigint not null,
    dispatch_expires_at datetime(6) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine = InnoDB default character set utf8mb4;

-- 기존 발송 행은 null을 유지한다. notification-processing-state SQL이 배포 전에 기존 null PENDING에 8분 기한을 고정한다.
-- 새 일반 알림의 null 기한은 업무상 만료 없음이며, 관리자는 별도 24시간 기한을 지정한다. JPA 관계·FK·cascade는 두지 않는다.
alter table notifications
    add column batch_id varchar(36) null,
    add column dispatch_expires_at datetime(6) null,
    add index idx_notification_batch (batch_id, status);

-- 운영 추적 예시. 토큰·payload 원문은 출력하지 않는다.
-- select id, requested_by_user_id, target_type, target_user_count, recipient_user_count,
--        enqueued_count, skipped_no_token_user_count, skipped_inactive_user_count, created_at
-- from notification_batches where id = :batch_id;
-- select status, result_code, count(*) from notifications
-- where batch_id = :batch_id group by status, result_code;

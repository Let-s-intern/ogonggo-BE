-- LC-3489 알림 기능 최초 배포용 통합 마이그레이션. MySQL 8에서 한 번만 실행한다.
-- 대상: 이 브랜치 이전 배포까지 완료되어 users.fcm_token과 기존 notifications가 있는 DB.
-- 포함: 다중 기기 토큰, 댓글·관리자 알림함, 관리자 발송 묶음, 토픽 구독, PROCESSING 복구.
-- 이전 개별 SQL을 일부라도 적용한 DB에는 이 파일을 그대로 실행하지 않는다.
-- 로컬 ddl-auto=update/create-drop으로 이미 새 스키마를 만든 DB도 실행 대상이 아니다.
--
-- 실행 전: DB 백업·기존 스키마 확인. 아래 전환 절차는 변경 유실을 막아야 할 때 적용한다.
-- 소규모 초기 배포에서 토큰 변경 누락·알림 실패를 허용한다면 쓰기·발송 차단을 생략하고,
-- SQL 선적용 → 기존 워크플로로 두 API 병렬 배포 → 구버전 종료·토큰 등록·알림 발송 확인으로 진행한다.
-- 변경 유실을 막는 전환: 토큰 PUT·DELETE와 알림 발송 중단, 진행 중 요청·발송 종료 확인.
-- 댓글 알림 누락도 막아야 한다면 배포가 끝날 때까지 댓글 쓰기를 중단한다.
-- notificationDelivery의 기존 enabled 값을 기록하고, 새 API 검증 후 그 값으로 복원한다.
-- MySQL DDL은 자동 커밋된다. 파일 전체가 하나의 트랜잭션으로 롤백되는 것은 아니다.
-- 오류가 나면 즉시 중단한다(mysql --force 사용 금지). 적용 내역 확인 없이 재실행하지 않는다.
--
-- 변경 유실을 막는 전환의 실행 후: 사용자 API 배포·구버전 종료 → 관리자 API 배포 → 검증 후 쓰기·발송 재개.
-- 토픽 전체 발송은 최초 구독·실기기 수신 확인 전까지 broadcast.enabled=false를 유지한다.
-- 새 버전에서 토큰을 변경한 뒤에는 users.fcm_token이 최신 상태가 아니므로 구버전으로 단순 롤백하지 않는다.
-- 상세 절차·토픽 확인 SQL: docs/architecture/notification-extension-guide.md의 브랜치 최초 배포.

-- 1. 다중 기기 토큰과 기존 토큰 이관
create table user_fcm_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    device_id varchar(36) null,
    token varchar(4096) null,
    token_hash varchar(64) character set ascii collate ascii_bin null,
    active_slot varchar(64) character set ascii collate ascii_bin null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,
    primary key (id),
    constraint uk_user_fcm_token_hash unique (token_hash),
    constraint uk_user_fcm_token_slot unique (active_slot),
    index idx_user_fcm_token_user (user_id, deleted_at, updated_at, id)
) engine = InnoDB;

alter table notifications modify column recipient_address varchar(4096) null;

-- 여러 회원에게 저장된 토큰은 소유자를 추측하지 않고 모두 제외한다. 원문은 출력하지 않는다.
select count(*) as excluded_duplicate_token_count
from (
    select sha2(fcm_token, 256)
    from users
    where fcm_token is not null and char_length(trim(fcm_token)) > 0
    group by sha2(fcm_token, 256)
    having count(*) > 1
) duplicates;

-- users.fcm_token은 수정·제거하지 않는다. 제외한 중복 토큰은 다음 클라이언트 PUT으로 등록한다.
start transaction;
insert into user_fcm_tokens (user_id, device_id, token, token_hash, active_slot, created_at, updated_at, deleted_at)
select u.id, null, u.fcm_token, sha2(u.fcm_token, 256), concat('legacy:', u.id),
       u.created_at, u.updated_at, null
from users u
join (
    select sha2(fcm_token, 256) as token_hash
    from users
    where fcm_token is not null and char_length(trim(fcm_token)) > 0
    group by sha2(fcm_token, 256)
    having count(*) = 1
) unique_tokens on unique_tokens.token_hash = sha2(u.fcm_token, 256)
where u.fcm_token is not null and char_length(trim(u.fcm_token)) > 0;
commit;

select count(*) as migrated_token_count from user_fcm_tokens where deleted_at is null;

-- 2. 댓글 알림함과 기기별 발송 연결. 기존 댓글·알림은 소급 생성하지 않는다.
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

-- 기존 발송 행은 null 유지. 발송 기록은 독립적으로 정리하므로 FK/cascade는 두지 않는다.
alter table notifications
    add column user_notification_id bigint null,
    add index idx_notification_user_notification (user_notification_id);

-- 3. 관리자 발송 묶음과 업무상 발송 기한
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

-- 기존 null PENDING의 8분 기한은 마지막 단계에서 고정한다. 새 관리자 발송은 24시간 기한이다.
alter table notifications
    add column batch_id varchar(36) null,
    add column dispatch_expires_at datetime(6) null,
    add index idx_notification_batch (batch_id, status);

-- 4. 관리자 발송을 사용자 알림함에도 표시
alter table user_notifications
    modify column post_id bigint null,
    modify column comment_id bigint null,
    add column batch_id varchar(36) null,
    add column url text null,
    add constraint uk_user_notification_batch_recipient unique (batch_id, recipient_user_id);

-- 댓글의 batch_id·url은 null이며 기존 댓글 중복 방지 제약을 유지한다.
-- 이전 관리자 발송의 알림함은 소급 생성하지 않는다.

-- 5. 전체 토픽 발송·공통 알림함·토픽 구독 동기화
alter table notifications
    add column recipient_type varchar(20) not null default 'DIRECT';

alter table notification_batches
    add column topic_name varchar(200) character set ascii collate ascii_bin null,
    add column url varchar(2048) null,
    add index idx_notification_broadcast_created (created_at, id);

create table notification_broadcast_reads (
    id bigint not null auto_increment,
    user_id bigint not null,
    batch_id varchar(36) not null,
    read_at datetime(6) not null,
    primary key (id),
    constraint uk_broadcast_read_user_batch unique (user_id, batch_id)
) engine=InnoDB default charset=utf8mb4;

create table fcm_topic_sync_tasks (
    id bigint not null auto_increment,
    topic varchar(200) character set ascii collate ascii_bin not null,
    token_hash varchar(64) character set ascii collate ascii_bin not null,
    token varchar(4096) null,
    desired_subscribed boolean not null,
    pending boolean not null,
    revision bigint not null,
    attempt_count int not null,
    last_error_code varchar(100) null,
    next_attempt_at datetime(6) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_fcm_topic_sync_token unique (topic, token_hash),
    index idx_fcm_topic_sync_due (pending, next_attempt_at, id)
) engine=InnoDB default charset=utf8mb4;

-- 최초 토픽 구독은 새 사용자 API가 수행한다. 이 SQL만으로 구독·전체 발송을 활성화하지 않는다.
-- 구독 완료 확인은 애플리케이션 배포 뒤 가이드의 운영 topic을 지정한 조회로 수행한다.

-- 6. PROCESSING 선점과 provider별 stale 복구
alter table notifications
    add column processing_attempt_id varchar(36) null,
    add column processing_started_at datetime(6) null,
    add column first_attempt_at datetime(6) null,
    add column attempt_count int not null default 0,
    add index idx_notification_processing_stale (status, channel, processing_started_at, id);

-- 새 일반 알림의 null 기한은 업무상 만료 없음이다.
-- 기존 null PENDING에는 과거 8분 발송 창을 고정해 오래된 대기열의 재발송을 막는다.
update notifications
set dispatch_expires_at = timestampadd(minute, 8, greatest(scheduled_at, created_at))
where status = 'PENDING'
  and dispatch_expires_at is null;

-- 운영 확인 예시. 수신 주소·토큰·payload는 출력하지 않는다.
-- select status, channel, count(*) from notifications group by status, channel;
-- select id, user_notification_id, batch_id, status, result_code, result_category, updated_at
-- from notifications order by id desc limit 100;
-- select id, channel, processing_started_at, attempt_count, result_code
-- from notifications where status in ('PROCESSING', 'UNKNOWN') order by id desc limit 100;

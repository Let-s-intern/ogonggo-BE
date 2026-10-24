-- MySQL 8. 기존 알림·관리자 배치·알림함 스키마 적용 후 한 번만 실행한다.
-- 운영 DB에서 실행한 기록이 아니다. DDL은 자동 커밋되므로 백업과 잠금 영향을 확인한다.
-- 스키마 → 사용자 API/클라이언트 → 최초 토픽 구독 → 관리자 ALL 전환 순서다.
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

-- 기존 배치는 topic_name=null이므로 DIRECT이며 회원별 알림함과 결과를 그대로 조회한다.
-- 기본 broadcast.enabled=false 동안 기존 ALL 직접 발송을 유지한다.
-- 두 API의 ogonggo.notification.broadcast.topic에 동일한 환경별 토픽 이름을 설정한다.
-- 먼저 사용자 API의 fcmTopicSync를 실행해 기존 활성 토큰의 최초 구독을 완료한다.
-- 아래 두 결과가 0이고 Firebase/클라이언트 수신을 확인한 뒤 관리자 API의 broadcast.enabled=true로 전환한다.
select count(*) as missing_active_subscriptions
from user_fcm_tokens t join users u on u.id=t.user_id
left join fcm_topic_sync_tasks s on s.token_hash=t.token_hash and s.topic='REPLACE_WITH_ENV_TOPIC'
where t.deleted_at is null and t.token is not null and u.status='ACTIVE'
  and (s.id is null or s.desired_subscribed=false or s.pending=true);
select count(*) as pending_sync_count from fcm_topic_sync_tasks
where topic='REPLACE_WITH_ENV_TOPIC' and pending=true;

-- 실패는 last_error_code·attempt_count·next_attempt_at, 진행은 pending·원하는 구독 상태로 추적한다.
-- token 원문을 운영 로그/조회 결과에 출력하지 않는다. 완료 시 token을 null로 지운다.
-- 롤백: 우선 ALL 전환 게이트를 끄고 토픽 작업을 처리할 수 있는 사용자 API는 유지한다.
-- TOPIC 대기열이 남아 있는 동안 이를 모르는 구버전 발송기로 되돌리지 않는다.
-- 새 컬럼·테이블·공통 공지·읽음 기록을 제거하지 않는다.

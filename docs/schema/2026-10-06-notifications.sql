-- 알림 기능 배포용 통합 스키마 마이그레이션.
-- 초기 배포용이다. 운영 DB에 일부 스키마가 이미 반영됐는지 사전 확인한 뒤 사용한다.
-- 운영 ddl-auto=none. 사용자 API 배포 전에 한 번 적용하며 자동 적용되지 않는다.

-- 재스크랩 시 bookmark.created_at은 유지되므로 현재 활성화 시각을 별도 관리한다.
-- 기존 북마크는 기존 생성 시각으로 초기화해 현재 데이터의 의미를 보존한다.
alter table job_bookmarks
    add column active_since datetime(6) null;

update job_bookmarks
set active_since = created_at
where active_since is null;

alter table job_bookmarks
    modify column active_since datetime(6) not null;

create table notifications (
    id bigint not null auto_increment,
    deduplication_key varchar(200) not null,
    channel varchar(20) not null,
    template_code varchar(100) not null,
    recipient_user_id bigint null,
    recipient_address varchar(512) null,
    payload_json text null,
    scheduled_at datetime(6) not null,
    status varchar(20) not null,
    provider_message_id varchar(255) null,
    sent_at datetime(6) null,
    result_code varchar(100) null,
    result_category varchar(50) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_notification_deduplication_key (deduplication_key),
    key idx_notification_due (status, scheduled_at, id),
    key idx_notification_cleanup (status, updated_at, id)
);

-- 발송 관련 두 작업은 비활성으로, 30일 보관 정리는 활성으로 미리 만든다.
-- NHN clip_remind 승인 전에는 코드도 reminder job 등록을 보류한다. 승인 후 코드 복구와 DB 활성화를 별도로 한다.
insert ignore into scheduled_jobs (
    name,
    cron,
    enabled,
    description,
    created_at,
    updated_at
)
values
    (
        'jobBookmarkAlimTalkReminder',
        '0 * * * * *',
        b'0',
        '스크랩한 채용공고의 D-1 알림톡 대기열 등록 (매분)',
        now(6),
        now(6)
    ),
    (
        'notificationDelivery',
        '* * * * * *',
        b'0',
        'due notification 발송 (매초, 다중 인스턴스는 ShedLock으로 직렬화)',
        now(6),
        now(6)
    ),
    (
        'notificationCleanup',
        '0 30 3 * * *',
        b'1',
        '최종 상태로 바뀐 지 30일 지난 알림 정리 (매일 03:30)',
        now(6),
        now(6)
    );

-- 회원 종류 토픽·희망 직무·광고성 관리자 발송과 발송 지연 경보 작업. 사용자·관리자 API 배포 전에 운영 DB에 먼저 적용한다.

-- 개별 알림(batch_id null)을 관리자 발송보다 먼저 꺼내는 조회가 인덱스 순서로 읽고 limit 건에서 멈추게 한다.
-- 이 인덱스가 없으면 관리자 발송 대기 행이 많을 때 개별 알림을 꺼낼 때마다 대기 행 전체를 읽는다.
create index idx_notification_due_direct on notifications (status, batch_id, scheduled_at, id);

-- 관리자 발송 기록에 희망 직무 대상·광고성 여부·동의하지 않아 뺀 회원 수를 남긴다.
-- target_type은 varchar(20)이라 새 값(GENERAL_MEMBERS, COMPANY_MEMBERS, JOB_ROLES)을 위한 변경은 없다.
-- 기존 행은 광고성이 아니고 동의로 뺀 회원이 없으므로 기본값 false·0으로 채운다.
alter table notification_batches
    add column target_job_roles_json text null,
    add column advertisement boolean not null default false,
    add column skipped_no_marketing_consent_user_count bigint not null default 0;

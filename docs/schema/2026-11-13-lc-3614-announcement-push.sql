-- 공지를 노출할 때 푸시 알림을 보낸다(LC-3614).
-- push_target 이 NONE 이 아니면 노출 상태가 되는 순간 한 번 보내고, 보낸 발송 묶음 식별자와 일시를 남긴다.
-- 이미 있는 공지는 NONE 이 되어 다시 노출해도 푸시를 보내지 않는다.

-- 2026-11-12-lc-3614-announcement-category-thumbnail-summary.sql 다음에, 관리자 API 배포 전에 적용한다
alter table announcements
    add column push_target varchar(30) not null default 'NONE',
    add column push_batch_id varchar(36) null,
    add column pushed_at datetime(6) null;

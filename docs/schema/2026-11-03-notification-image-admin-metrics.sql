-- 관리자 푸시 이미지와 기간별 읽음 통계. 관리자 API 배포 전에 운영 DB에 적용한다.
-- 기존 배치는 이미지가 없으므로 두 컬럼이 null이다.
alter table notification_batches
    add column image_id varchar(36) null,
    add column image_url varchar(2048) null;

-- 회원별 공통 알림 읽음의 배치 단위 집계에 사용한다.
create index idx_notification_broadcast_read_batch on notification_broadcast_reads (batch_id);

-- 알림 provider 오류 상세 저장 컬럼 추가.
-- 운영 DB에 notifications 테이블을 이미 생성한 경우 사용자 API 배포 전에 한 번 실행한다.
-- provider 원문 전체가 아니라 sender가 민감정보를 제거한 상세만 최대 500자로 저장한다.
alter table notifications
    add column result_detail varchar(500) null after result_category;

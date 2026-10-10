-- 사용자별 현재 FCM 토큰 저장. 여러 기기 지원이 필요해지면 별도 user_fcm_tokens 테이블로 확장한다.
-- 사용자 API 배포 전에 운영 DB에 한 번 적용한다. 자동 적용되지 않는다.
alter table users
    add column fcm_token varchar(4096) null;

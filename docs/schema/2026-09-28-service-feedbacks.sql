-- 2026-09-28 서비스 개선 의견 테이블
--
-- 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다. 이미 있으면 `if not exists`로 건너뛴다.
-- 로그인 없이도 남길 수 있어 user_id는 비어 있을 수 있다. 한 사용자가 여러 번 남길 수 있어 유니크 제약이 없다.

create table if not exists service_feedbacks
(
    id           bigint        not null auto_increment,
    user_id      bigint,
    satisfaction varchar(1000),
    improvement  varchar(1000),
    created_at   datetime(6)   not null,
    updated_at   datetime(6)   not null,
    primary key (id)
);

-- 2026-09-30 오늘의 공고 테이블
--
-- 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다. 이미 있으면 `if not exists`로 건너뛴다.
-- 운영자가 관리자 콘솔에서 고른 채용공고를 노출 순서와 함께 둔다. 삭제되지 않은 행 전체가 지금의 오늘의 공고다.
-- 목록을 바꾸면 기존 행을 소프트 삭제하고 새 행을 넣으므로 같은 공고의 지난 행이 여러 개 남는다. 그래서 유니크 제약이 없다.

create table if not exists today_jobs
(
    id            bigint      not null auto_increment,
    job_id        bigint      not null,
    display_order int         not null,
    deleted_at    datetime(6),
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    primary key (id),
    index idx_today_jobs_active (deleted_at, display_order)
);

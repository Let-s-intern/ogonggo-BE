-- 2026-09-27 스케줄 작업 설정 테이블
--
-- 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다. 이미 있으면 `if not exists`로 건너뛴다.
-- 행은 애플리케이션이 기동할 때 코드의 기본 cron으로 만든다. 여기서 미리 넣지 않아도 된다.

create table if not exists scheduled_jobs
(
    id          bigint       not null auto_increment,
    name        varchar(64)  not null,
    cron        varchar(100) not null,
    enabled     bit          not null,
    description varchar(200) not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    constraint uk_scheduled_job_name unique (name)
);

-- 운영 예시
-- 주기 변경(1분 안에 반영): update scheduled_jobs set cron = '0 0 3 * * *' where name = 'work24DailyCollection';
-- 끄기(다음 회차부터 건너뜀): update scheduled_jobs set enabled = false where name = 'work24DailyCollection';

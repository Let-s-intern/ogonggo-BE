-- 2026-09-14 관리자 콘솔: 검수 상태, 부트캠프 게시 상태, 반려 기록
--
-- 기존 DB에 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
-- 사용자·관리자 API를 새 코드로 배포하기 전에 백업한 뒤 실행한다.
-- bootcamps.publication_status는 NOT NULL enum 칼럼이다. Hibernate update에 맡기면 기존 행에 빈 문자열이 들어가 enum 매핑이 깨진다.
--
-- enum 칼럼 타입은 Hibernate 6의 MySQL 기본 매핑(enum)에 맞췄다.
-- 기존 칼럼이 varchar라면 `show create table jobs`로 확인하고 같은 타입으로 바꿔 실행한다.

-- 1. 상시 채용인데 모집 종료 일시가 있는 행을 먼저 확인한다.
--    새 도메인 규칙은 이 조합을 허용하지 않아, 이런 행을 기업회원이 수정하면 실패한다.
--    0건이 아니면 종료 일시를 지울지 기간 채용으로 바꿀지 담당자와 정한 뒤 진행한다.
select 'jobs' as target, id, recruitment_start_at, recruitment_end_at
from jobs
where recruitment_type = 'ALWAYS_OPEN'
  and recruitment_end_at is not null
union all
select 'bootcamps' as target, id, recruitment_start_at, recruitment_end_at
from bootcamps
where recruitment_type = 'ALWAYS_OPEN'
  and recruitment_end_at is not null;

-- 2. 채용공고 검수 상태
--    크롤링 수집분(owner_user_id가 없는 행)은 검수 대상이 아니므로 null로 둔다.
--    이미 게시 중인 기업회원 공고는 승인으로 보고 노출을 유지하고, 게시 전인 공고는 검수 대기로 둔다.
alter table jobs
    add column review_status enum ('PENDING', 'APPROVED', 'REJECTED') null;

update jobs
set review_status = case when publication_status = 'PUBLISHED' then 'APPROVED' else 'PENDING' end
where owner_user_id is not null;

create index idx_jobs_review on jobs (review_status, deleted_at);

-- 3. 부트캠프 게시 상태
--    지금까지는 모집 중·모집 마감이면 공개였으므로 그 행을 게시로 둔다. 공개 기간 조건은 계속 따로 적용된다.
alter table bootcamps
    add column publication_status enum ('DRAFT', 'PUBLISHED', 'HIDDEN', 'ARCHIVED') null;

update bootcamps
set publication_status = case when status in ('RECRUITING', 'CLOSED') then 'PUBLISHED' else 'DRAFT' end;

alter table bootcamps
    modify column publication_status enum ('DRAFT', 'PUBLISHED', 'HIDDEN', 'ARCHIVED') not null;

-- 4. 부트캠프 검수 상태. 채용공고와 같은 기준으로 채운다.
alter table bootcamps
    add column review_status enum ('PENDING', 'APPROVED', 'REJECTED') null;

update bootcamps
set review_status = case when publication_status = 'PUBLISHED' then 'APPROVED' else 'PENDING' end
where owner_user_id is not null;

create index idx_bootcamps_review on bootcamps (review_status, deleted_at);

-- 5. 반려 기록
create table content_rejections
(
    id                bigint                     not null auto_increment,
    content_type      enum ('JOB', 'BOOTCAMP')   not null,
    content_id        bigint                     not null,
    reason            varchar(1000)              not null,
    rejected_at       datetime(6)                not null,
    reason_updated_at datetime(6),
    deleted_at        datetime(6),
    created_at        datetime(6)                not null,
    updated_at        datetime(6)                not null,
    primary key (id),
    constraint uk_content_rejections_content unique (content_type, content_id)
) engine = InnoDB;

create index idx_content_rejections_active_latest on content_rejections (deleted_at, rejected_at);

-- 6. 확인
select owner_user_id is not null as company, publication_status, review_status, count(*)
from jobs
where deleted_at is null
group by company, publication_status, review_status;

select owner_user_id is not null as company, status, publication_status, review_status, count(*)
from bootcamps
where deleted_at is null
group by company, status, publication_status, review_status;

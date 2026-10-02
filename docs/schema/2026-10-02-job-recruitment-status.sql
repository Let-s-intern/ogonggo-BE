-- 2026-10-02 채용공고 모집 상태(recruitment_status)
--
-- 모집 상태를 조회할 때마다 계산하지 않고 저장한다. 등록·수정·마감할 때 정하고,
-- 모집 종료 일시가 지난 공고는 사용자 API의 매시 자동 마감 작업(`jobAutoClose`)이 CLOSED로 바꾼다.
-- 사용자·관리자 API를 새 코드로 배포하기 전에 백업한 뒤 한 번만 실행한다. 새 코드는 이 칼럼으로 목록을 거르고
-- 공고를 저장할 때 값을 넣으므로 칼럼이 없으면 공고 등록과 목록 조회가 실패한다.
-- 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

-- 1. 칼럼을 먼저 null 허용으로 추가한다.
alter table jobs
    add column recruitment_status varchar(20) null;

-- 2. 기존 공고는 이전 계산식과 같게 채운다. 종료 일시와 같은 시각까지는 모집 중이다.
update jobs
set recruitment_status = if(closed_at is not null or recruitment_end_at < now(), 'CLOSED', 'RECRUITING');

-- 3. 빈 값이 없는지 확인한 뒤 not null을 건다. 0건이어야 한다.
select count(*) from jobs where recruitment_status is null;

alter table jobs
    modify column recruitment_status varchar(20) not null;

-- 4. 자동 마감 작업이 모집 중이면서 종료 일시가 지난 공고를 찾는 인덱스다.
create index idx_jobs_recruitment_status_end on jobs (recruitment_status, recruitment_end_at);

-- 2026-09-30 채용공고 목록 정렬 키(list_sort_key)
--
-- 공개 목록 최신순이 크롤러 공고를 앞에 두고, 같은 날 등록한 공고를 섞도록 등록할 때 정렬 키를 저장한다.
-- 사용자·관리자 API를 새 코드로 배포하기 전에 백업한 뒤 한 번만 실행한다. 새 코드는 이 칼럼에 값을 넣어 저장하고
-- 목록을 이 칼럼으로 정렬하므로 칼럼이 없으면 공고 등록과 목록 조회가 실패한다.
-- 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
--
-- 키 구성은 `JobListSortKey`와 같다: [크롤러 여부 1비트][등록일(1970-01-01부터의 일수) 20비트][무작위 32비트]

-- 1. 칼럼을 먼저 null 허용으로 추가한다.
alter table jobs
    add column list_sort_key bigint null;

-- 2. 기존 공고는 생성 일시의 날짜를 등록일로 보고 채운다. 같은 날 공고끼리는 무작위 값으로 섞인다.
update jobs
set list_sort_key = if(source = 'CRAWLER', 1 << 52, 0)
    | (datediff(date(created_at), '1970-01-01') << 32)
    | floor(rand() * 4294967296);

-- 3. 빈 값이 없는지 확인한 뒤 not null을 건다. 0건이어야 한다.
select count(*) from jobs where list_sort_key is null;

alter table jobs
    modify column list_sort_key bigint not null;

-- 4. 최신순 인덱스를 정렬 키를 포함한 인덱스로 바꾼다. 새 인덱스가 앞 두 칼럼을 그대로 가지므로 기존 인덱스는 지운다.
create index idx_jobs_published_list_sort on jobs (publication_status, deleted_at, list_sort_key);

drop index idx_jobs_published_latest on jobs;

-- 2026-09-17 채용공고 직군 필터 인덱스
--
-- 공고 목록과 북마크 목록이 직군(job_field)으로 거를 수 있게 되어 직무·산업 필터와 같은 모양의 인덱스를 추가한다.
-- 기존 DB에 한 번만 실행한다. 신규 DB는 Hibernate가 엔티티의 인덱스 선언으로 만든다.
-- 칼럼 변경이 없는 인덱스 추가라 배포 전후 어느 쪽에 실행해도 기존 코드가 깨지지 않는다.

-- 1. 같은 이름의 인덱스가 이미 있는지 확인한다. ddl-auto=update로 먼저 만들어졌으면 2는 건너뛴다.
show index from jobs where key_name = 'idx_jobs_published_job_field';

-- 2. 인덱스 추가
create index idx_jobs_published_job_field on jobs (publication_status, deleted_at, job_field);

-- 2026-10-02 관리자 채용공고 목록의 직군·직무 필터 인덱스
--
-- 관리자 목록은 게시 상태와 무관하게 미삭제 공고를 읽으므로 게시 상태로 시작하는 공개 목록 인덱스를 쓸 수 없다.
-- 직군(job_field)과 직무(job_role) 필터마다 미삭제 조건을 붙인 인덱스를 둔다. 뒤에 붙는 기본 키로 id 역순 정렬도 인덱스를 따른다.
-- 사용자·관리자 API를 새 코드로 배포하기 전에 한 번만 실행한다. 인덱스가 없어도 조회는 되지만 전체를 읽는다.
-- 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

create index idx_jobs_job_field on jobs (job_field, deleted_at);

create index idx_jobs_job_role on jobs (job_role, deleted_at);

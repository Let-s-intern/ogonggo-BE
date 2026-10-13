-- 채용공고 원문을 jobs 밖 job_sources 로 옮긴다. 원문은 이제 본문 영역 HTML 이다.
-- jobs.source_text 는 글자만 편 것이라 쓰지 않는다. 옮기지 않고 지운다. 크롤러가 HTML 을 다시 채운다.
-- 긴 원문이 jobs 행에 있으면 공고 목록 정렬이 sort_buffer_size 를 넘겨 "Out of sort memory" 가 난다.

-- 1. 관리자·사용자 API 배포 전에 적용한다
create table job_sources (
    id bigint not null auto_increment,
    job_id bigint not null,
    html longtext not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_job_sources_job_id unique (job_id)
);

-- 2. 두 API 배포가 끝난 뒤 적용한다. 먼저 지우면 아직 떠 있는 이전 버전이 source_text 를 읽다 실패한다
alter table jobs drop column source_text;

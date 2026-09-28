-- 2026-09-24 기업 로고와 부트캠프 강사 정보·교육 특징·수료 조건 칼럼 추가
--
-- 2026-09-14에 기업 로고를 공고 대표 이미지(cover_image_url)로 합치고 company_logo_url을 지웠다.
-- 기업회원 등록 화면이 로고와 대표 이미지를 따로 받으므로 jobs·bootcamps에 logo_url을 다시 둔다.
-- 수집한 공고는 여전히 로고를 대표 이미지로 받으므로 logo_url이 비어 있다.
--
-- 추가만 하는 변경이라 DDL_AUTO=update 환경은 Hibernate가 칼럼을 만든다.
-- DDL_AUTO=validate로 운영하는 DB에만 새 코드를 배포하기 전에 한 번 실행한다. 모든 칼럼이 null 허용이라 기존 행은 바뀌지 않는다.

-- 1. 이미 추가된 칼럼이 있는지 확인한다. 결과가 있으면 해당 칼럼은 빼고 실행한다.
show columns from jobs like 'logo_url';
show columns from bootcamps where Field in ('logo_url', 'instructor_info', 'program_features', 'completion_requirements');

-- 2. 칼럼 추가
alter table jobs
    add column logo_url varchar(2048) null;

alter table bootcamps
    add column logo_url                varchar(2048) null,
    add column instructor_info         longtext      null,
    add column program_features        longtext      null,
    add column completion_requirements longtext      null;

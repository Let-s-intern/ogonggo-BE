-- 2026-09-21 마이페이지 지원·신청 관리: 채용공고·부트캠프 북마크의 지원 단계
--
-- 기존 DB에 한 번만 적용한다. 신규 DB는 Hibernate가 최종 스키마를 만든다.
-- 사용자·관리자 API를 새 코드로 배포하기 전에 실행한다.
-- DDL_AUTO=update 환경에서 이 SQL 없이 배포하면 Hibernate가 기본값 없이 not null 칼럼을 추가해
-- 기존 행이 빈 문자열이 되고, 그 북마크를 읽을 때 enum 변환이 실패한다.
-- 기존 북마크는 모두 스크랩(SCRAPPED) 단계로 채운다.

set @job_bookmark_status_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'job_bookmarks'
      and column_name = 'application_status'
);

set @job_bookmark_status_ddl = if(
    @job_bookmark_status_exists = 0,
    'alter table job_bookmarks add column application_status varchar(30) not null default ''SCRAPPED''',
    'select ''job_bookmarks.application_status already exists'''
);

prepare job_bookmark_status_statement from @job_bookmark_status_ddl;
execute job_bookmark_status_statement;
deallocate prepare job_bookmark_status_statement;

set @bootcamp_bookmark_status_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'bootcamp_bookmarks'
      and column_name = 'application_status'
);

set @bootcamp_bookmark_status_ddl = if(
    @bootcamp_bookmark_status_exists = 0,
    'alter table bootcamp_bookmarks add column application_status varchar(30) not null default ''SCRAPPED''',
    'select ''bootcamp_bookmarks.application_status already exists'''
);

prepare bootcamp_bookmark_status_statement from @bootcamp_bookmark_status_ddl;
execute bootcamp_bookmark_status_statement;
deallocate prepare bootcamp_bookmark_status_statement;

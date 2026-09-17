-- 2026-09-17 커뮤니티 댓글 소프트 삭제
--
-- 기존 DB에 한 번만 적용한다. 신규 DB는 Hibernate가 최종 스키마를 만든다.
-- DDL_AUTO=update가 실행되는 환경에서는 Hibernate가 이 nullable 컬럼을 추가하므로 이 SQL을 실행하지 않는다.
-- DDL_AUTO=validate 또는 외부 스키마 관리 환경은 코드 배포 전에 컬럼을 준비한다.

set @comment_deleted_at_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'recruitment_post_comments'
      and column_name = 'deleted_at'
);

set @comment_deleted_at_ddl = if(
    @comment_deleted_at_exists = 0,
    'alter table recruitment_post_comments add column deleted_at datetime(6) null',
    'select ''recruitment_post_comments.deleted_at already exists'''
);

prepare comment_deleted_at_statement from @comment_deleted_at_ddl;
execute comment_deleted_at_statement;
deallocate prepare comment_deleted_at_statement;

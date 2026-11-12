-- 공지사항에 카테고리·썸네일·요약을 더한다(LC-3614).
-- 이미 있는 공지는 카테고리가 오공고 업데이트(OGONGGO_UPDATE)가 된다. 썸네일과 요약은 비어 있다.

-- 사용자·관리자 API 배포 전에 적용한다. 칼럼이 없으면 공지를 읽는 모든 요청이 실패한다
alter table announcements
    add column category varchar(30) not null default 'OGONGGO_UPDATE',
    add column thumbnail_url varchar(2048) null,
    add column summary varchar(300) null;

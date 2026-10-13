-- 크롤러가 원문 페이지에서 읽은 글을 공고에 함께 저장한다. 배포 전에 운영 DB에 먼저 적용한다.
alter table jobs add column source_text longtext null;

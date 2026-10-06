-- 2026-10-06 공지 테이블 이름을 notices에서 announcements로 변경
--
-- 알림(notification)과 헷갈리지 않도록 공지를 announcement로 부른다. 코드·API 경로(/api/v1/announcements)와 테이블 이름을 맞춘다.
-- 새 서버는 announcements를, 지금 서버는 notices를 읽으므로 이름을 바꾸는 순간 둘 중 하나는 테이블을 찾지 못한다.
-- 그래서 이름을 바꾸면서 예전 이름의 뷰를 두어, 두 API 배포가 끝날 때까지 지금 서버도 같은 행을 읽고 쓰게 한다.
--   1~3 실행 → 두 API 배포 → 4 실행
-- 뷰를 거친 쓰기는 관리자 공지 작성·수정뿐이므로, 1과 배포 사이에는 공지를 작성하지 않는다.
-- 백업한 뒤 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

-- 1. 테이블 이름을 바꾼다.
rename table notices to announcements;

-- 2. 엔티티의 인덱스 이름과 맞춘다.
alter table announcements rename index idx_notices_public to idx_announcements_public;

-- 3. 지금 서버가 배포 전까지 쓰는 예전 이름이다. 한 테이블의 모든 칼럼을 그대로 보이므로 조회와 수정을 그대로 받는다.
create view notices as select * from announcements;

-- ↓ 두 API 배포 후 실행

-- 4. 예전 이름을 지운다.
drop view notices;

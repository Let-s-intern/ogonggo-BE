-- 2026-10-06 모집글 포지션에서 모바일(MOBILE)을 빼고 마케팅(MARKETING)을 추가
--
-- 화면의 포지션 선택지가 백엔드·프론트엔드·디자인·기획·마케팅·기타로 바뀐다. 이미 모바일로 저장된 포지션은 마케팅으로 옮긴다.
-- 기존 DB의 position 칼럼은 Hibernate 6의 MySQL 기본 매핑(enum)으로 만들어져 있어, 칼럼에 MARKETING을 먼저 넣은 뒤
-- 값을 옮기고 MOBILE을 뺀다. MOBILE이 남은 채로 새 코드를 배포하면 그 모집글을 읽을 때 오류가 나므로 배포 전에 실행한다.
-- 백업한 뒤 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

-- 1. 지금 칼럼 정의와 옮길 행 수를 확인한다. Type이 enum이 아니라 varchar이면 2와 4는 건너뛰고 3만 실행한다.
show columns from community_post_positions like 'position';
select count(*) from community_post_positions where position = 'MOBILE';

-- 2. MOBILE과 MARKETING을 모두 받을 수 있게 넓힌다.
alter table community_post_positions
    modify column position
        enum ('BACKEND', 'FRONTEND', 'DESIGN', 'PM', 'MOBILE', 'MARKETING', 'ETC') not null;

-- 3. 모바일 포지션을 마케팅으로 옮긴다. MARKETING은 새 값이라 같은 글에 겹치는 행이 없다.
update community_post_positions
set position = 'MARKETING'
where position = 'MOBILE';

-- 4. RecruitmentPostPosition의 값 전부로 바꾼다. 3에서 MOBILE이 모두 옮겨졌으므로 남은 값은 그대로 유지된다.
alter table community_post_positions
    modify column position
        enum ('BACKEND', 'FRONTEND', 'DESIGN', 'PM', 'MARKETING', 'ETC') not null;

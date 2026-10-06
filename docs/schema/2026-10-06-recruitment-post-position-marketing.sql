-- 2026-10-06 모집글 포지션에 마케팅(MARKETING) 추가 (서버 1차 배포)
--
-- 화면의 포지션 선택지가 백엔드·프론트엔드·디자인·기획·마케팅·기타로 바뀐다. 프런트가 옮겨 가는 동안 이전 화면이 보내는
-- 모바일(MOBILE)과 기존 모집글의 MOBILE도 계속 받아야 하므로, 이 단계에서는 칼럼에 MARKETING만 추가하고 값은 옮기지 않는다.
-- MOBILE을 MARKETING으로 옮기고 칼럼에서 빼는 일은 프런트 배포 후 2026-10-06-recruitment-post-position-mobile-cleanup.sql로 한다.
--
-- 기존 DB의 position 칼럼은 Hibernate 6의 MySQL 기본 매핑(enum)으로 만들어져 있어, 목록에 없는 값을 넣으면 저장이 실패한다.
-- 서버 1차 배포 전에 한 번만 실행한다. 칼럼을 넓히기만 하므로 지금 배포된 서버에도 영향이 없다.
-- 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

-- 1. 지금 칼럼 정의를 확인한다. Type이 enum이 아니라 varchar이면 2는 건너뛴다.
show columns from community_post_positions like 'position';

-- 2. MOBILE을 남긴 채 MARKETING을 받을 수 있게 넓힌다. 기존 값은 그대로 남는다.
alter table community_post_positions
    modify column position
        enum ('BACKEND', 'FRONTEND', 'DESIGN', 'PM', 'MOBILE', 'MARKETING', 'ETC') not null;

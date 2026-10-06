-- 2026-10-06 모집글 포지션에서 모바일(MOBILE) 제거 (서버 2차 배포)
--
-- 프런트가 마케팅(MARKETING)으로 옮겨 간 뒤, 저장된 MOBILE을 MARKETING으로 옮기고 칼럼에서 MOBILE을 뺀다.
-- 2026-10-06-recruitment-post-position-marketing.sql을 먼저 적용했어야 한다.
--
-- MOBILE이 남은 채 RecruitmentPostPosition에서 MOBILE을 뺀 서버를 배포하면 그 모집글을 읽을 때 오류가 난다.
-- 반대로 MOBILE을 옮긴 뒤에도 이전 화면을 연 사용자가 MOBILE을 다시 저장할 수 있다. 그래서 순서를 지킨다.
--   1~2 실행 → 서버 2차 배포(MOBILE 제거) → 3~4 실행
-- 백업한 뒤 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.

-- 1. 옮길 행 수를 확인한다.
select count(*) from community_post_positions where position = 'MOBILE';

-- 2. 모바일 포지션을 마케팅으로 옮긴다.
--    전환 기간에 새 화면에서 MOBILE이 있던 글에 마케팅을 더하면 한 글에 둘이 함께 있다. 그대로 옮기면 마케팅이 두 번 남고,
--    중복 포지션은 수정 요청에서 거절되므로 그런 글은 MOBILE 행을 지운다. 컬렉션 테이블이라 행 단위 이력은 없다.
delete mobile
from community_post_positions mobile
    join community_post_positions marketing
        on marketing.post_id = mobile.post_id and marketing.position = 'MARKETING'
where mobile.position = 'MOBILE';

update community_post_positions
set position = 'MARKETING'
where position = 'MOBILE';

-- ↓ 서버 2차 배포 후 바로 실행

-- 3. 2와 배포 사이에 이전 화면에서 다시 저장된 MOBILE을 같은 방법으로 옮긴다.
--    배포 직후 이 행이 있는 글은 3을 마칠 때까지 읽을 때 오류가 나므로 배포와 3 사이를 비우지 않는다.
delete mobile
from community_post_positions mobile
    join community_post_positions marketing
        on marketing.post_id = mobile.post_id and marketing.position = 'MARKETING'
where mobile.position = 'MOBILE';

update community_post_positions
set position = 'MARKETING'
where position = 'MOBILE';

-- 4. RecruitmentPostPosition의 값 전부로 바꾼다. MOBILE이 남아 있으면 이 문장이 실패하므로 3을 먼저 실행한다.
alter table community_post_positions
    modify column position
        enum ('BACKEND', 'FRONTEND', 'DESIGN', 'PM', 'MARKETING', 'ETC') not null;

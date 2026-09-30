-- 2026-09-30 부트캠프 기업 연계 과정 표시
--
-- 공개 목록(GET /api/v1/bootcamps)은 기업과 함께 운영하는 과정을 어느 정렬에서든 먼저 보여 준다.
-- bootcamps.enterprise_linked: 기업 연계 과정이면 1. 등록할 때 정해지며 수정으로는 바뀌지 않는다.
--   지금은 고용24 수집만 1로 넣는다. 과정명 앞 괄호의 기업 이름이 이름표(Work24PartnerCompanies)에 있을 때다.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다.
-- 칼럼이 없으면 부트캠프 조회와 등록이 모두 실패한다. 기존 행은 0이라 지금 순서 그대로다.

-- 1. 이미 적용됐는지 확인한다. 결과가 있으면 실행하지 않는다.
show columns from bootcamps where Field = 'enterprise_linked';

-- 2. 기업 연계 과정 여부
alter table bootcamps
    add column enterprise_linked bit(1) not null default b'0';

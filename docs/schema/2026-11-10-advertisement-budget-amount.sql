-- 광고 신청 폼의 예정 예산을 구간(budget_range)에서 신청자가 적는 원 단위 금액(budget_amount)으로 바꾼다.
-- 1) 배포 전에 적용한다. 선택 입력이고 비즈니스 공고 계약과 기존 행은 값이 없어 nullable이다.
alter table advertisement_contracts add column budget_amount int null;

-- 2) 배포가 끝난 뒤 적용한다. 배포 전 서버는 아직 budget_range를 읽으므로 먼저 지우면 안 된다.
--    구간 값은 금액으로 옮길 수 없어 옮기지 않는다. 지우기 전에 남길 값이 있는지 확인한다.
--    select id, budget_range from advertisement_contracts where budget_range is not null;
alter table advertisement_contracts drop column budget_range;

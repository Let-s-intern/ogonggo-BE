-- LC-3591 광고 신청 폼에서 고르는 예정 예산 구간을 advertisement_contracts에 더한다. 배포 전에 운영 DB에 먼저 적용한다.
-- 선택 입력이고 비즈니스 공고 계약과 기존 행은 값이 없어 nullable이다. enum 값은 다른 업무 enum과 같이 이름을 varchar로 저장한다.
alter table advertisement_contracts add column budget_range varchar(30) null;
